
@Grab("org.slf4j:jul-to-slf4j:1.7.25")
@Grab("org.slf4j:log4j-over-slf4j:1.7.25")

@Grab('com.fasterxml.jackson.core:jackson-databind:2.9.10.5')
@Grab('com.fasterxml.jackson.core:jackson-core:2.9.7')
@Grab('com.fasterxml.jackson.core:jackson-annotations:2.9.7')
@Grab('ch.qos.logback:logback-classic:1.2.3')

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer
import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.util.logging.Slf4j


/**
 * The base class for the F1 2018 Telemetry app. Starts up a non-blocking I/O
 * UDP server to read packets from the F1 2018 video game and then hands those
 * packets off to a parallel thread for processing based on the lambda function
 * defined. Leverages a fluent API for initialization.
 *
 * Also exposes a main method for starting up a default server
 *
 * @author eh7n
 *
 */
@Slf4j
@groovy.transform.CompileStatic
class Main {

//	private static final Logger log = LoggerFactory.getLogger(Main.class)

	private static final String BINDTO_ADDRESS = "0.0.0.0"
	private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0"
	private static final int DEFAULT_PORT = 20777
	private static final int MAX_PACKET_SIZE = 1341

	private String bindAddress
	private int port
	private Consumer<Packet> packetConsumer

	private Main() {
		bindAddress = DEFAULT_BIND_ADDRESS
		port = DEFAULT_PORT
	}

	/**
	 * Create an instance of the UDP server
	 *
	 * @return
	 */
	public static Main create() {
		return new Main()
	}

	/**
	 * Set the bind address
	 *
	 * @param bindAddress
	 * @return the server instance
	 */
	Main bindTo(String bindAddress) {
		this.bindAddress = bindAddress
		return this
	}

	/**
	 * Set the bind port
	 *
	 * @param port
	 * @return the server instance
	 */
	Main onPort(int port) {
		this.port = port
		return this
	}

	/**
	 * Set the consumer via a lambda function
	 *
	 * @param consumer
	 * @return the server instance
	 */
	Main consumeWith(Consumer<Packet> consumer) {
		packetConsumer = consumer
		return this
	}

	/**
	 * Start the F1 2018 Telemetry UDP server
	 *
	 * @throws IOException           if the server fails to start
	 * @throws IllegalStateException if you do not define how the packets should be
	 *                               consumed
	 */
	public void start() throws IOException {

		if (packetConsumer == null) {
			throw new IllegalStateException("You must define how the packets will be consumed.")
		}

		log.info("F1 2018 - Telemetry UDP Server")

		// Create an executor to process the Packets in a separate thread
		// To be honest, this is probably an over-optimization due to the use of NIO,
		// but it was done to provide a simple way of providing back pressure on the
		// incoming UDP packet handling to allow for long-running processing of the
		// Packet object, if required.
		ExecutorService executor = Executors.newSingleThreadExecutor()

		try {
      DatagramChannel channel = DatagramChannel.open()
			channel.socket().bind(new InetSocketAddress(bindAddress, port))
			log.info("Listening on " + bindAddress + ":" + port + "...")
			ByteBuffer buf = ByteBuffer.allocate(MAX_PACKET_SIZE)
			buf.order(ByteOrder.LITTLE_ENDIAN)
			while (true) {
				channel.receive(buf)
				final Packet packet = PacketDeserializer.read(buf.array())
				executor.submit({
					packetConsumer.accept(packet)
				})
				buf.clear()
			}
		} finally {
			executor.shutdown()
		}
	}

	/**
	 * Main class in case you want to run the server independently. Uses defaults
	 * for bind address and port, and just logs the incoming packets as a JSON
	 * object to the location defined in the logback config
	 *
	 * @param args
	 * @throws IOException
	 */
	static void main(String[] args) throws IOException {
		Main.create()
							.bindTo(BINDTO_ADDRESS)
							.onPort(20777)
							.consumeWith{ def p ->
									log.trace(p.toJSON())
								}
							.start()
	}
}



// file: WheelData.groovy
@groovy.transform.CompileStatic
public class WheelData<T> {

	private T rearLeft;
	private T rearRight;
	private T frontLeft;
	private T frontRight;

	public WheelData() {}

	public WheelData(T[] datapoints) {
		this.setRearLeft(datapoints[0]);
		this.setRearRight(datapoints[1]);
		this.setFrontLeft(datapoints[2]);
		this.setFrontRight(datapoints[3]);
	}

	public WheelData(T rl, T rr, T fl, T fr) {
		this.setRearLeft(rl);
		this.setRearRight(rr);
		this.setFrontLeft(fl);
		this.setFrontRight(fr);
	}

	public T getRearLeft() {
		return rearLeft;
	}
	public void setRearLeft(T rearLeft) {
		this.rearLeft = rearLeft;
	}
	public T getRearRight() {
		return rearRight;
	}
	public void setRearRight(T rearRight) {
		this.rearRight = rearRight;
	}
	public T getFrontLeft() {
		return frontLeft;
	}
	public void setFrontLeft(T frontLeft) {
		this.frontLeft = frontLeft;
	}
	public T getFrontRight() {
		return frontRight;
	}
	public void setFrontRight(T frontRight) {
		this.frontRight = frontRight;
	}

}


// file: DriverStatus.groovy
@groovy.transform.CompileStatic
public enum DriverStatus {

	IN_GARAGE,
	FLYING_LAP,
	IN_LAP,
	OUT_LAP,
	ON_TRACK;

	public static DriverStatus fromInt(int i) {
		switch(i) {
			case 0:
				return DriverStatus.IN_GARAGE;
			case 1:
				return DriverStatus.FLYING_LAP;
			case 2:
				return DriverStatus.IN_LAP;
			case 3:
				return DriverStatus.OUT_LAP;
			default:
				return DriverStatus.ON_TRACK;
		}
	}
}


// file: PacketParticipantsData.groovy
@groovy.transform.CompileStatic

public class PacketParticipantsData extends Packet {

	private int numCars;
	private List<ParticipantData> participants;

	public PacketParticipantsData() {}

	public int getNumCars() {
		return numCars;
	}
	public void setNumCars(int numCars) {
		this.numCars = numCars;
	}
	public List<ParticipantData> getParticipants() {
		return participants;
	}
	public void setParticipants(List<ParticipantData> participants) {
		this.participants = participants;
	}
}


// file: PacketDeserializer.groovy

/**
 * F1 2018 PacketDeserializer is the main class for deserializing the incoming
 * UDP packets and building Packet POJOs from the byte arrays
 *
 * This class was created based on the documented UDP Specification located on
 * the Codemasters forums.
 *
 * @author eh7n
 * @see https://forums.codemasters.com/discussion/136948/f1-2018-udp-specification
 *
 */
 @groovy.transform.CompileStatic
public class PacketDeserializer {

	public static final int TOTAL_NBR_CARS=20;
	public static final int MAX_NBR_MARSHAL_ZONES=21;

	private PacketBuffer buffer;

	private PacketDeserializer(def data) {
		buffer = PacketBuffer.wrap(data);
	}

	/**
	 * Read the packet data from a byte array
	 *
	 * @param data : a F1 2018 UDP packet
	 * @return a Packet POJO
	 */
	public static Packet read(byte[] data) {
		return new PacketDeserializer(data).buildPacket();
	}

	private Packet buildPacket() {

		Header header = buildHeader();

		switch (header.getPacketId()) {
		case 0:
			return buildPacketMotionData(header);
		case 1:
			return buildPacketSessionData(header);
		case 2:
			return buildPacketLapData(header);
		case 3:
			return buildPacketEventData(header);
		case 4:
			return buildPacketParticipantsData(header);
		case 5:
			return buildPacketCarSetupData(header);
		case 6:
			return buildPacketCarTelemetryData(header);
		case 7:
			return buildPacketCarStatusData(header);
		}

		return null;
	}

	/**
	 * HEADER
	 *
	 * Each packet has the following header
	 *
	 * <pre>
	 * {@code
	 	struct PacketHeader
		{
			uint16    m_packetFormat;         // 2018
			uint8     m_packetVersion;        // Version of this packet type, all start from 1
			uint8     m_packetId;             // Identifier for the packet type, see below
			uint64    m_sessionUID;           // Unique identifier for the session
			float     m_sessionTime;          // Session timestamp
			uint      m_frameIdentifier;      // Identifier for the frame the data was retrieved on
			uint8     m_playerCarIndex;       // Index of player's car in the array
		};
	 * }
	 * </pre>
	 *
	 * @return the Header pojo
	 */
	private Header buildHeader() {

		Header header = new Header();

		header.setPacketFormat(buffer.getNextUInt16AsInt()); // 2
		header.setPacketVersion(buffer.getNextUInt8AsInt()); // 1
		header.setPacketId(buffer.getNextUInt8AsInt()); // 1
		header.setSessionUID(buffer.getNextUInt64AsBigInteger()); // 8
		header.setSessionTime(buffer.getNextFloat());// 4
		header.setFrameIdentifier(buffer.getNextUIntAsLong());// 4
		header.setPlayerCarIndex(buffer.getNextUInt8AsInt()); // 1
		return header;
	}

	/**
	 * LAP DATA PACKET
	 *
	 * The lap data packet gives details of all the cars in the session.
	 *
	 * Frequency: Rate as specified in menus
	 *
	 * Size: 841 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketLapData
		{
			PacketHeader    m_header;              // Header
			LapData         m_lapData[20];         // Lap data for all cars on track
		};
	 * }
	 * </pre>
	 *
	 * @return the PacketLapData pojo
	 */
	private PacketLapData buildPacketLapData(Header header) {

		PacketLapData packetData = new PacketLapData();
		List<LapData> lapDataList = new ArrayList<>();

		int i = 0;
		int playersIndex = header.getPlayerCarIndex();
		while (i < TOTAL_NBR_CARS) {
			lapDataList.add(buildLapData(i, i == playersIndex));
			i++;
		}

		packetData.setHeader(header);
		packetData.setLapDataList(lapDataList);
		return packetData;
	}

	/**
	 * LAP DATA
	 *
	 * <pre>
	 * {@code
		struct LapData
		{
		    float       m_lastLapTime;           // Last lap time in seconds
		    float       m_currentLapTime;        // Current time around the lap in seconds
		    float       m_bestLapTime;           // Best lap time of the session in seconds
		    float       m_sector1Time;           // Sector 1 time in seconds
		    float       m_sector2Time;           // Sector 2 time in seconds
		    float       m_lapDistance;           // Distance vehicle is around current lap in metres – could
		                                         // be negative if line hasn’t been crossed yet
		    float       m_totalDistance;         // Total distance travelled in session in metres – could
		                                         // be negative if line hasn’t been crossed yet
		    float       m_safetyCarDelta;        // Delta in seconds for safety car
		    uint8       m_carPosition;           // Car race position
		    uint8       m_currentLapNum;         // Current lap number
		    uint8       m_pitStatus;             // 0 = none, 1 = pitting, 2 = in pit area
		    uint8       m_sector;                // 0 = sector1, 1 = sector2, 2 = sector3
		    uint8       m_currentLapInvalid;     // Current lap invalid - 0 = valid, 1 = invalid
		    uint8       m_penalties;             // Accumulated time penalties in seconds to be added
		    uint8       m_gridPosition;          // Grid position the vehicle started the race in
		    uint8       m_driverStatus;          // Status of driver - 0 = in garage, 1 = flying lap
		                                         // 2 = in lap, 3 = out lap, 4 = on track
		    uint8       m_resultStatus;          // Result status - 0 = invalid, 1 = inactive, 2 = active
		                                         // 3 = finished, 4 = disqualified, 5 = not classified
		                                         // 6 = retired
		};
	 * }
	 * </pre>
	 */
	private LapData buildLapData(int carIndex, boolean playersCar) {

		LapData lapData = new LapData();

		lapData.setCarIndex(carIndex);
		lapData.setPlayersCar(playersCar);
		lapData.setLastLapTime(buffer.getNextFloat());
		lapData.setCurrentLapTime(buffer.getNextFloat());
		lapData.setBestLaptTime(buffer.getNextFloat());
		lapData.setSector1Time(buffer.getNextFloat());
		lapData.setSector2Time(buffer.getNextFloat());
		lapData.setLapDistance(buffer.getNextFloat());
		lapData.setTotalDistance(buffer.getNextFloat());
		lapData.setSafetyCarDelta(buffer.getNextFloat());
		lapData.setCarPosition(buffer.getNextUInt8AsInt());
		lapData.setCurrentLapNum(buffer.getNextUInt8AsInt());
		lapData.setPitStatus(PitStatus.fromInt(buffer.getNextUInt8AsInt()));
		lapData.setSector(buffer.getNextUInt8AsInt() + 1);
		lapData.setCurrentLapInvalid(buffer.getNextUInt8AsInt() == 1);
		lapData.setPenalties(buffer.getNextUInt8AsInt());
		lapData.setGridPosition(buffer.getNextUInt8AsInt());
		lapData.setDriverStatus(DriverStatus.fromInt(buffer.getNextUInt8AsInt()));
		lapData.setResultStatus(ResultStatus.fromInt(buffer.getNextUInt8AsInt()));

		return lapData;
	}

	/**
	 * MOTION PACKET
	 *
	 * The motion packet gives physics data for all the cars being driven. There is
	 * additional data for the car being driven with the goal of being able to drive
	 * a motion platform setup.
	 *
	 * Frequency: Rate as specified in menus
	 *
	 * Size: 1341 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketMotionData
		{
		    PacketHeader    m_header;               	// Header

		    CarMotionData   m_carMotionData[20];		// Data for all cars on track

		    // Extra player car ONLY data
		    float         m_suspensionPosition[4];       // Note: All wheel arrays have the following order:
		    float         m_suspensionVelocity[4];       // RL, RR, FL, FR
		    float         m_suspensionAcceleration[4];   // RL, RR, FL, FR
		    float         m_wheelSpeed[4];               // Speed of each wheel
		    float         m_wheelSlip[4];                // Slip ratio for each wheel
		    float         m_localVelocityX;              // Velocity in local space
		    float         m_localVelocityY;              // Velocity in local space
		    float         m_localVelocityZ;              // Velocity in local space
		    float         m_angularVelocityX;            // Angular velocity x-component
		    float         m_angularVelocityY;            // Angular velocity y-component
		    float         m_angularVelocityZ;            // Angular velocity z-component
		    float         m_angularAccelerationX;        // Angular velocity x-component
		    float         m_angularAccelerationY;        // Angular velocity y-component
		    float         m_angularAccelerationZ;        // Angular velocity z-component
		    float         m_frontWheelsAngle;            // Current front wheels angle in radians
		};
	 * }
	 * </pre>
	 *
	 * @return the PacketMotionData pojo
	 */
	private PacketMotionData buildPacketMotionData(Header header) {

		PacketMotionData packetMotionData = new PacketMotionData();

		packetMotionData.setHeader(header);
		List<CarMotionData> carMotionDataList = new ArrayList<>();
		int carIndex = 0;
		int playersCarIndex = packetMotionData.getHeader().getPlayerCarIndex();
		while (carIndex < TOTAL_NBR_CARS) {
			carMotionDataList.add(buildCarMotionData(carIndex, carIndex == playersCarIndex));
			carIndex++;
		}
		packetMotionData.setCarMotionDataList(carMotionDataList);
		packetMotionData.setSuspensionPosition(new WheelData<Float>(buffer.getNextFloatArray(4)));
		packetMotionData.setSuspensionVelocity(new WheelData<Float>(buffer.getNextFloatArray(4)));
		packetMotionData.setSuspensionAcceleration(new WheelData<Float>(buffer.getNextFloatArray(4)));
		packetMotionData.setWheelSpeed(new WheelData<Float>(buffer.getNextFloatArray(4)));
		packetMotionData.setWheelSlip(new WheelData<Float>(buffer.getNextFloatArray(4)));
		packetMotionData.setLocalVelocityX(buffer.getNextFloat());
		packetMotionData.setLocalVelocityY(buffer.getNextFloat());
		packetMotionData.setLocalVelocityZ(buffer.getNextFloat());
		packetMotionData.setAngularVelocityX(buffer.getNextFloat());
		packetMotionData.setAngularVelocityY(buffer.getNextFloat());
		packetMotionData.setAngularVelocityZ(buffer.getNextFloat());
		packetMotionData.setAngularAccelerationX(buffer.getNextFloat());
		packetMotionData.setAngularAccelerationY(buffer.getNextFloat());
		packetMotionData.setAngularAccelerationZ(buffer.getNextFloat());
		packetMotionData.setFrontWheelsAngle(buffer.getNextFloat());

		return packetMotionData;
	}

	/**
	 * CAR MOTION DATA
	 *
	 * N.B. For the normalised vectors below, to convert to float values divide by
	 * 32767.0f. 16-bit signed values are used to pack the data and on the
	 * assumption that direction values are always between -1.0f and 1.0f.
	 *
	 * <pre>
	 * {@code
		struct CarMotionData
		{
		    float         m_worldPositionX;           // World space X position
		    float         m_worldPositionY;           // World space Y position
		    float         m_worldPositionZ;           // World space Z position
		    float         m_worldVelocityX;           // Velocity in world space X
		    float         m_worldVelocityY;           // Velocity in world space Y
		    float         m_worldVelocityZ;           // Velocity in world space Z
		    int16         m_worldForwardDirX;         // World space forward X direction (normalised)
		    int16         m_worldForwardDirY;         // World space forward Y direction (normalised)
		    int16         m_worldForwardDirZ;         // World space forward Z direction (normalised)
		    int16         m_worldRightDirX;           // World space right X direction (normalised)
		    int16         m_worldRightDirY;           // World space right Y direction (normalised)
		    int16         m_worldRightDirZ;           // World space right Z direction (normalised)
		    float         m_gForceLateral;            // Lateral G-Force component
		    float         m_gForceLongitudinal;       // Longitudinal G-Force component
		    float         m_gForceVertical;           // Vertical G-Force component
		    float         m_yaw;                      // Yaw angle in radians
		    float         m_pitch;                    // Pitch angle in radians
		    float         m_roll;                     // Roll angle in radians
		};
	 * }
	 * </pre>
	 */
	private CarMotionData buildCarMotionData(int carIndex, boolean playersCar) {

		final float denormalizer = 32767.0f;

		CarMotionData carMotionData = new CarMotionData();

		carMotionData.setCarIndex(carIndex);
		carMotionData.setPlayersCar(playersCar);
		carMotionData.setWorldPositionX(buffer.getNextFloat());
		carMotionData.setWorldPositionY(buffer.getNextFloat());
		carMotionData.setWorldPositionZ(buffer.getNextFloat());
		carMotionData.setWorldVelocityX(buffer.getNextFloat());
		carMotionData.setWorldVelocityY(buffer.getNextFloat());
		carMotionData.setWorldVelocityZ(buffer.getNextFloat());
		carMotionData.setWorldForwardDirX((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setWorldForwardDirY((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setWorldForwardDirZ((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setWorldRightDirX((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setWorldRightDirY((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setWorldRightDirZ((buffer.getNextUInt16AsInt() / denormalizer).toFloat());
		carMotionData.setgForceLateral(buffer.getNextFloat());
		carMotionData.setgForceLongitudinal(buffer.getNextFloat());
		carMotionData.setgForceVertical(buffer.getNextFloat());
		carMotionData.setYaw(buffer.getNextFloat());
		carMotionData.setPitch(buffer.getNextFloat());
		carMotionData.setRoll(buffer.getNextFloat());

		return carMotionData;

	}

	/**
	 * SESSION PACKET
	 *
	 * The session packet includes details about the current session in progress.
	 *
	 * Frequency: 2 per second
	 *
	 * Size: 147 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketSessionData
		{
		    PacketHeader    m_header;               // Header

		    uint8           m_weather;              // Weather - 0 = clear, 1 = light cloud, 2 = overcast
		                                            // 3 = light rain, 4 = heavy rain, 5 = storm
		    int8	    	m_trackTemperature;    	// Track temp. in degrees celsius
		    int8	    	m_airTemperature;      	// Air temp. in degrees celsius
		    uint8           m_totalLaps;           	// Total number of laps in this race
		    uint16          m_trackLength;          // Track length in metres
		    uint8           m_sessionType;         	// 0 = unknown, 1 = P1, 2 = P2, 3 = P3, 4 = Short P
		                                            // 5 = Q1, 6 = Q2, 7 = Q3, 8 = Short Q, 9 = OSQ
		                                            // 10 = R, 11 = R2, 12 = Time Trial
		    int8            m_trackId;         		// -1 for unknown, 0-21 for tracks, see appendix
		    uint8           m_era;                  // Era, 0 = modern, 1 = classic
		    uint16          m_sessionTimeLeft;    	// Time left in session in seconds
		    uint16          m_sessionDuration;     	// Session duration in seconds
		    uint8           m_pitSpeedLimit;      	// Pit speed limit in kilometres per hour
		    uint8           m_gamePaused;           // Whether the game is paused
		    uint8           m_isSpectating;        	// Whether the player is spectating
		    uint8           m_spectatorCarIndex;  	// Index of the car being spectated
		    uint8           m_sliProNativeSupport;	// SLI Pro support, 0 = inactive, 1 = active
		    uint8           m_numMarshalZones;      // Number of marshal zones to follow
		    MarshalZone     m_marshalZones[21];     // List of marshal zones – max 21
		    uint8           m_safetyCarStatus;      // 0 = no safety car, 1 = full safety car
		                                            // 2 = virtual safety car
		    uint8          m_networkGame;           // 0 = offline, 1 = online
		};
	 * }
	 * </pre>
	 */
	private PacketSessionData buildPacketSessionData(Header header) {

		PacketSessionData sessionData = new PacketSessionData();

		sessionData.setHeader(header);
		sessionData.setWeather(Weather.fromInt(buffer.getNextUInt8AsInt()));
		sessionData.setTrackTemperature(buffer.getNextInt8AsInt());
		sessionData.setAirTemperature(buffer.getNextInt8AsInt());
		sessionData.setTotalLaps(buffer.getNextUInt8AsInt());
		sessionData.setTrackLength(buffer.getNextUInt16AsInt());
		sessionData.setSessionType(SessionType.fromInt(buffer.getNextUInt8AsInt()));
		sessionData.setTrackId(buffer.getNextInt8AsInt());
		sessionData.setEra(Era.fromInt(buffer.getNextInt8AsInt()));
		sessionData.setSessionTimeLeft(buffer.getNextUInt16AsInt());
		sessionData.setSessionDuration(buffer.getNextUInt16AsInt());
		sessionData.setPitSpeedLimit(buffer.getNextUInt8AsInt());
		sessionData.setGamePaused(buffer.getNextUInt8AsBoolean());
		sessionData.setSpectating(buffer.getNextUInt8AsBoolean());
		sessionData.setSliProNativeSupport(buffer.getNextUInt8AsBoolean());
		sessionData.setNumMarshalZones(buffer.getNextInt8AsInt());
		sessionData.setMarshalZones(buildMarshalZones());
		sessionData.setSafetyCarStatus(SafetyCarStatus.fromInt(buffer.getNextUInt8AsInt()));
		sessionData.setNetworkGame(buffer.getNextUInt8AsBoolean());

		return sessionData;
	}

	/**
	 * MARSHAL ZONE
	 *
	 * <pre>
	 * {@code
		struct MarshalZone
		{
		    float  m_zoneStart;   // Fraction (0..1) of way through the lap the marshal zone starts
		    int8   m_zoneFlag;    // -1 = invalid/unknown, 0 = none, 1 = green, 2 = blue, 3 = yellow, 4 = red
		};
	 * }
	 * </pre>
	 */
	private List<MarshalZone> buildMarshalZones() {
		List<MarshalZone> marshalZones = new ArrayList<>();
		for (int k = 0; k < MAX_NBR_MARSHAL_ZONES; k++) {
			MarshalZone marshalZone = new MarshalZone();
			marshalZone.setZoneStart(buffer.getNextFloat());
			marshalZone.setZoneFlag(ZoneFlag.fromInt(buffer.getNextInt8AsInt()));
			marshalZones.add(marshalZone);
		}
		return marshalZones;
	}

	/**
	 * EVENT PACKET
	 *
	 * This packet gives details of events that happen during the course of the
	 * race.
	 *
	 * Frequency: When the event occurs
	 *
	 * Size: 25 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketEventData
		{
		    PacketHeader    m_header;               // Header

		    uint8           m_eventStringCode[4];   // Event string code, see above
		};
	 * }
	 * </pre>
	 *
	 * @param header
	 * @return the EventData packet
	 */
	private PacketEventData buildPacketEventData(Header header) {
		PacketEventData eventData = new PacketEventData();
		eventData.setHeader(header);
		eventData.setEventCode(buffer.getNextCharArrayAsString(4));

		return eventData;
	}

	/**
	 * PARTICIPANTS PACKET
	 *
	 * This is a list of participants in the race. If the vehicle is controlled by
	 * AI, then the name will be the driver name. If this is a multiplayer game, the
	 * names will be the Steam Id on PC, or the LAN name if appropriate. On Xbox
	 * One, the names will always be the driver name, on PS4 the name will be the
	 * LAN name if playing a LAN game, otherwise it will be the driver name.
	 *
	 * Frequency: Every 5 seconds
	 *
	 * Size: 1082 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketParticipantsData
		{
		    PacketHeader    m_header;            // Header

		    uint8           m_numCars;           // Number of cars in the data
		    ParticipantData m_participants[20];
		};
	 * }
	 * </pre>
	 *
	 * @param header
	 * @return a PacketParticipantsData pojo
	 *
	 */
	private PacketParticipantsData buildPacketParticipantsData(Header header) {

		PacketParticipantsData participantsData = new PacketParticipantsData();
		participantsData.setHeader(header);
		participantsData.setNumCars(buffer.getNextUInt8AsInt());
		List<ParticipantData> participants = new ArrayList<>();
		for (int k = 0; k < participantsData.getNumCars(); k++) {
			participants.add(buildParticipantData());
		}
		participantsData.setParticipants(participants);
		// Ignore the rest of the data in the buffer
		return participantsData;
	}

	/**
	 * PARTICIPANT DATA
	 *
	 * <pre>
	 * {@code
		struct ParticipantData
		{
		    uint8      m_aiControlled;           // Whether the vehicle is AI (1) or Human (0) controlled
		    uint8      m_driverId;               // Driver id - see appendix
		    uint8      m_teamId;                 // Team id - see appendix
		    uint8      m_raceNumber;             // Race number of the car
		    uint8      m_nationality;            // Nationality of the driver
		    char       m_name[48];               // Name of participant in UTF-8 format – null terminated
		                                         // Will be truncated with … (U+2026) if too long
	 * }; }
	 *
	 * @return a ParticipantData pojo
	 */
	private ParticipantData buildParticipantData() {
		ParticipantData participant = new ParticipantData();
		participant.setAiControlled(buffer.getNextUInt8AsBoolean());
		participant.setDriverId(buffer.getNextUInt8AsInt());
		participant.setTeamId(buffer.getNextUInt8AsInt());
		participant.setRaceNumber(buffer.getNextUInt8AsInt());
		participant.setNationality(buffer.getNextUInt8AsInt());
		participant.setName(buffer.getNextCharArrayAsString(48));
		return participant;
	}

	/**
	 * CAR SETUPS PACKET
	 *
	 * This packet details the car setups for each vehicle in the session. Note that
	 * in multiplayer games, other player cars will appear as blank, you will only
	 * be able to see your car setup and AI cars.
	 *
	 * Frequency: Every 5 seconds
	 *
	 * Size: 841 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketCarSetupData
		{
		    PacketHeader    m_header;            // Header

		    CarSetupData    m_carSetups[20];
		};
	 * }
	 * </pre>
	 *
	 * @param header
	 * @return
	 */
	private PacketCarSetupData buildPacketCarSetupData(Header header) {
		PacketCarSetupData carSetupData = new PacketCarSetupData();
		carSetupData.setHeader(header);
		List<CarSetupData> carSetups = new ArrayList<>();
		for (int k = 0; k < TOTAL_NBR_CARS; k++) {
			carSetups.add(buildCarSetupData());
		}
		carSetupData.setCarSetups(carSetups);
		return carSetupData;
	}

	/**
	 * CAR SETUP DATA
	 *
	 * <pre>
	 * {@code
		struct CarSetupData
		{
			uint8     m_frontWing;                // Front wing aero
			uint8     m_rearWing;                 // Rear wing aero
			uint8     m_onThrottle;               // Differential adjustment on throttle (percentage)
			uint8     m_offThrottle;              // Differential adjustment off throttle (percentage)
			float     m_frontCamber;              // Front camber angle (suspension geometry)
			float     m_rearCamber;               // Rear camber angle (suspension geometry)
			float     m_frontToe;                 // Front toe angle (suspension geometry)
			float     m_rearToe;                  // Rear toe angle (suspension geometry)
			uint8     m_frontSuspension;          // Front suspension
			uint8     m_rearSuspension;           // Rear suspension
			uint8     m_frontAntiRollBar;         // Front anti-roll bar
			uint8     m_rearAntiRollBar;          // Front anti-roll bar
			uint8     m_frontSuspensionHeight;    // Front ride height
			uint8     m_rearSuspensionHeight;     // Rear ride height
			uint8     m_brakePressure;            // Brake pressure (percentage)
			uint8     m_brakeBias;                // Brake bias (percentage)
			float     m_frontTyrePressure;        // Front tyre pressure (PSI)
			float     m_rearTyrePressure;         // Rear tyre pressure (PSI)
			uint8     m_ballast;                  // Ballast
			float     m_fuelLoad;                 // Fuel load
		};
	 * }
	 * </pre>
	 *
	 * @return a CarSetupData pojo
	 */
	private CarSetupData buildCarSetupData() {
		CarSetupData setupData = new CarSetupData();
		setupData.setFrontWing(buffer.getNextUInt8AsInt()); // 1*
		setupData.setRearWing(buffer.getNextUInt8AsInt()); // 2*
		setupData.setOnThrottle(buffer.getNextUInt8AsInt()); // 3*
		setupData.setOffThrottle(buffer.getNextUInt8AsInt()); // 4*
		setupData.setFrontCamber(buffer.getNextFloat()); // 8 *
		setupData.setRearCamber(buffer.getNextFloat()); // 16*
		setupData.setFrontToe(buffer.getNextFloat()); // 24*
		setupData.setRearToe(buffer.getNextFloat()); // 32*
		setupData.setFrontSuspension(buffer.getNextUInt8AsInt()); // 33*
		setupData.setRearSuspension(buffer.getNextUInt8AsInt()); // 34*
		setupData.setFrontAntiRollBar(buffer.getNextUInt8AsInt()); // 35*
		setupData.setRearAntiRollBar(buffer.getNextUInt8AsInt()); // 36*
		setupData.setFrontSuspensionHeight(buffer.getNextUInt8AsInt()); // 37*
		setupData.setRearSuspensionHeight(buffer.getNextUInt8AsInt()); // 38*
		setupData.setBrakePressure(buffer.getNextUInt8AsInt());
		setupData.setBrakeBias(buffer.getNextUInt8AsInt()); // 39
		setupData.setFrontTirePressure(buffer.getNextFloat()); // 47
		setupData.setRearTirePressure(buffer.getNextFloat()); // 55
		setupData.setBallast(buffer.getNextUInt8AsInt()); // 56
		setupData.setFuelLoad(buffer.getNextFloat()); // 40
		return setupData;
	}

	/**
	 * CAR TELEMETRY PACKET
	 *
	 * This packet details telemetry for all the cars in the race. It details
	 * various values that would be recorded on the car such as speed, throttle
	 * application, DRS etc.
	 *
	 * Frequency: Rate as specified in menus
	 *
	 * Size: 1085 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketCarTelemetryData
		{
		    PacketHeader        m_header;                // Header

		    CarTelemetryData    m_carTelemetryData[20];

		    uint32              m_buttonStatus;         // Bit flags specifying which buttons are being
		                                                // pressed currently - see appendices
		};
	 * }
	 * </pre>
	 *
	 * @param header
	 * @return a PacketCarTelemetryData pojo
	 */
	private PacketCarTelemetryData buildPacketCarTelemetryData(Header header) {
		PacketCarTelemetryData packetCarTelemetry = new PacketCarTelemetryData();
		packetCarTelemetry.setHeader(header);
		List<CarTelemetryData> carsTelemetry = new ArrayList<>();
		for (int k = 0; k < TOTAL_NBR_CARS; k++) {
			carsTelemetry.add(buildCarTelemetryData());
		}
		packetCarTelemetry.setButtonStatus(buildButtonStatus());
		packetCarTelemetry.setCarTelemetryData(carsTelemetry);
		return packetCarTelemetry;
	}

	/**
	 * CAR TELEMETRY DATA
	 *
	 * <pre>
	 * {@code
		struct CarTelemetryData
		{
		    uint16    m_speed;                      // Speed of car in kilometres per hour
		    uint8     m_throttle;                   // Amount of throttle applied (0 to 100)
		    int8      m_steer;                      // Steering (-100 (full lock left) to 100 (full lock right))
		    uint8     m_brake;                      // Amount of brake applied (0 to 100)
		    uint8     m_clutch;                     // Amount of clutch applied (0 to 100)
		    int8      m_gear;                       // Gear selected (1-8, N=0, R=-1)
		    uint16    m_engineRPM;                  // Engine RPM
		    uint8     m_drs;                        // 0 = off, 1 = on
		    uint8     m_revLightsPercent;           // Rev lights indicator (percentage)
		    uint16    m_brakesTemperature[4];       // Brakes temperature (celsius)
		    uint16    m_tyresSurfaceTemperature[4]; // Tyres surface temperature (celsius)
		    uint16    m_tyresInnerTemperature[4];   // Tyres inner temperature (celsius)
		    uint16    m_engineTemperature;          // Engine temperature (celsius)
		    float     m_tyresPressure[4];           // Tyres pressure (PSI)
		};
	 * }
	 * </pre>
	 *
	 * @return a CarTelemetryData pojo
	 */
	private CarTelemetryData buildCarTelemetryData() {

		CarTelemetryData carTelemetry = new CarTelemetryData();

		carTelemetry.setSpeed(buffer.getNextUInt16AsInt());
		carTelemetry.setThrottle(buffer.getNextUInt8AsInt());
		carTelemetry.setSteer(buffer.getNextInt8AsInt());
		carTelemetry.setBrake(buffer.getNextUInt8AsInt());
		carTelemetry.setClutch(buffer.getNextUInt8AsInt());
		carTelemetry.setGear(buffer.getNextInt8AsInt());
		carTelemetry.setEngineRpm(buffer.getNextUInt16AsInt());
		carTelemetry.setDrs(buffer.getNextUInt8AsBoolean());
		carTelemetry.setRevLightsPercent(buffer.getNextUInt8AsInt());
		carTelemetry.setBrakeTemperature(new WheelData<Integer>(buffer.getNextUInt16ArrayAsIntegerArray(4)));
		carTelemetry.setTireSurfaceTemperature(new WheelData<Integer>(buffer.getNextUInt16ArrayAsIntegerArray(4)));
		carTelemetry.setTireInnerTemperature(new WheelData<Integer>(buffer.getNextUInt16ArrayAsIntegerArray(4)));
		carTelemetry.setEngineTemperature(buffer.getNextUInt16AsInt());
		carTelemetry.setTirePressure(new WheelData<Float>(buffer.getNextFloatArray(4)));

		return carTelemetry;
	}

	/**
	 * BUTTON FLAGS
	 *
	 * These flags are used in the telemetry packet to determine if any buttons are
	 * being held on the controlling device. If the value below logical ANDed with
	 * the button status is set then the corresponding button is being held
	 *
	 * @return the ButtonStatus pojo
	 */
	private ButtonStatus buildButtonStatus() {

		long flags = buffer.getNextUIntAsLong();

		ButtonStatus controller = new ButtonStatus();
		controller.setCrossAPressed((flags & 0x0001) == 1);
		controller.setTriangleYPressed((flags & 0x0002) == 1);
		controller.setCircleBPressed((flags & 0x0004) == 1);
		controller.setSquareXPressed((flags & 0x0008) == 1);
		controller.setDpadLeftPressed((flags & 0x0010) == 1);
		controller.setDpadRightPressed((flags & 0x0020) == 1);
		controller.setDpadUpPressed((flags & 0x0040) == 1);
		controller.setDpadDownPressed((flags & 0x0080) == 1);
		controller.setOptionsMenuPressed((flags & 0x0100) == 1);
		controller.setL1LBPressed((flags & 0x0200) == 1);
		controller.setR1RBPressed((flags & 0x0400) == 1);
		controller.setL2LTPressed((flags & 0x0800) == 1);
		controller.setR2RTPressed((flags & 0x1000) == 1);
		controller.setLeftStickPressed((flags & 0x2000) == 1);
		controller.setRightStickPressed((flags & 0x4000) == 1);

		return controller;
	}

	/**
	 * CAR STATUS PACKET
	 *
	 * This packet details car statuses for all the cars in the race. It includes
	 * values such as the damage readings on the car.
	 *
	 * Frequency: 2 per second
	 *
	 * Size: 1061 bytes
	 *
	 * <pre>
	 * {@code
		struct PacketCarStatusData
		{
		    PacketHeader        m_header;            // Header

		    CarStatusData       m_carStatusData[20];
		};
	 * }
	 * </pre>
	 *
	 * @param header
	 * @return a PacketCarStatusData packet
	 */
	private PacketCarStatusData buildPacketCarStatusData(Header header) {

		PacketCarStatusData packetCarStatus = new PacketCarStatusData();

		packetCarStatus.setHeader(header);
		List<CarStatusData> carStatuses = new ArrayList<>();
		for (int k = 0; k < TOTAL_NBR_CARS; k++) {
			carStatuses.add(buildCarStatusData());
		}
		packetCarStatus.setCarStatuses(carStatuses);

		return packetCarStatus;
	}

	/**
	 * CAR STATUS DATA
	 *
	 * <pre>
	 * {@code
		struct CarStatusData
		{
		    uint8       m_tractionControl;          // 0 (off) - 2 (high)
		    uint8       m_antiLockBrakes;           // 0 (off) - 1 (on)
		    uint8       m_fuelMix;                  // Fuel mix - 0 = lean, 1 = standard, 2 = rich, 3 = max
		    uint8       m_frontBrakeBias;           // Front brake bias (percentage)
		    uint8       m_pitLimiterStatus;         // Pit limiter status - 0 = off, 1 = on
		    float       m_fuelInTank;               // Current fuel mass
		    float       m_fuelCapacity;             // Fuel capacity
		    uint16      m_maxRPM;                   // Cars max RPM, point of rev limiter
		    uint16      m_idleRPM;                  // Cars idle RPM
		    uint8       m_maxGears;                 // Maximum number of gears
		    uint8       m_drsAllowed;               // 0 = not allowed, 1 = allowed, -1 = unknown
		    uint8       m_tyresWear[4];             // Tyre wear percentage
		    uint8       m_tyreCompound;             // Modern - 0 = hyper soft, 1 = ultra soft
		                                            // 2 = super soft, 3 = soft, 4 = medium, 5 = hard
		                                            // 6 = super hard, 7 = inter, 8 = wet
		                                            // Classic - 0-6 = dry, 7-8 = wet
		    uint8       m_tyresDamage[4];           // Tyre damage (percentage)
		    uint8       m_frontLeftWingDamage;      // Front left wing damage (percentage)
		    uint8       m_frontRightWingDamage;     // Front right wing damage (percentage)
		    uint8       m_rearWingDamage;           // Rear wing damage (percentage)
		    uint8       m_engineDamage;             // Engine damage (percentage)
		    uint8       m_gearBoxDamage;            // Gear box damage (percentage)
		    uint8       m_exhaustDamage;            // Exhaust damage (percentage)
		    int8        m_vehicleFiaFlags;          // -1 = invalid/unknown, 0 = none, 1 = green
		                                            // 2 = blue, 3 = yellow, 4 = red
		    float       m_ersStoreEnergy;           // ERS energy store in Joules
		    uint8       m_ersDeployMode;            // ERS deployment mode, 0 = none, 1 = low, 2 = medium
		                                            // 3 = high, 4 = overtake, 5 = hotlap
		    float       m_ersHarvestedThisLapMGUK;  // ERS energy harvested this lap by MGU-K
		    float       m_ersHarvestedThisLapMGUH;  // ERS energy harvested this lap by MGU-H
		    float       m_ersDeployedThisLap;       // ERS energy deployed this lap
		};
	 * }
	 * </pre>
	 *
	 * @return a CarStatusData pojo
	 */
	private CarStatusData buildCarStatusData() {

		CarStatusData carStatus = new CarStatusData();

		carStatus.setTractionControl(buffer.getNextUInt8AsInt());
		carStatus.setAntiLockBrakes(buffer.getNextUInt8AsBoolean());
		carStatus.setFuelMix(buffer.getNextUInt8AsInt());
		carStatus.setFrontBrakeBias(buffer.getNextUInt8AsInt());
		carStatus.setPitLimiterOn(buffer.getNextUInt8AsBoolean());
		carStatus.setFuelInTank(buffer.getNextFloat());
		carStatus.setFuelCapacity(buffer.getNextFloat());
		carStatus.setMaxRpm(buffer.getNextUInt16AsInt());
		carStatus.setIdleRpm(buffer.getNextUInt16AsInt());
		carStatus.setMaxGears(buffer.getNextUInt8AsInt());
		carStatus.setDrsAllowed(buffer.getNextUInt8AsInt());
		carStatus.setTiresWear(new WheelData<Integer>(buffer.getNextUInt8ArrayAsIntegerArray(4)));
		carStatus.setTireCompound(buffer.getNextUInt8AsInt());
		carStatus.setTiresDamage(new WheelData<Integer>(buffer.getNextUInt8ArrayAsIntegerArray(4)));
		carStatus.setFrontLeftWheelDamage(buffer.getNextUInt8AsInt());
		carStatus.setFrontRightWingDamage(buffer.getNextUInt8AsInt());
		carStatus.setRearWingDamage(buffer.getNextUInt8AsInt());
		carStatus.setEngineDamage(buffer.getNextUInt8AsInt());
		carStatus.setGearBoxDamage(buffer.getNextUInt8AsInt());
		carStatus.setExhaustDamage(buffer.getNextUInt8AsInt());
		carStatus.setVehicleFiaFlags(buffer.getNextInt8AsInt());
		carStatus.setErsStoreEngery(buffer.getNextFloat());
		carStatus.setErsDeployMode(buffer.getNextUInt8AsInt());
		carStatus.setErsHarvestedThisLapMGUK(buffer.getNextFloat());
		carStatus.setErsHarvestedThisLapMGUH(buffer.getNextFloat());
		carStatus.setErsDeployedThisLap(buffer.getNextFloat());

		return carStatus;
	}
}


// file: Era.groovy
@groovy.transform.CompileStatic
public enum Era {

	MODERN,
	CLASSIC;

	public static Era fromInt(int i) {
		switch(i) {
			case 0:
				return Era.MODERN;
			case 1:
				return Era.CLASSIC;
			default:
				return null;
		}
	}
}


// file: CarMotionData.groovy
@groovy.transform.CompileStatic
public class CarMotionData {

	private int carIndex;
	private boolean playersCar;
	private float worldPositionX;
	private float worldPositionY;
	private float worldPositionZ;
	private float worldVelocityX;
	private float worldVelocityY;
	private float worldVelocityZ;
	private float worldForwardDirX;
	private float worldForwardDirY;
	private float worldForwardDirZ;
	private float worldRightDirX;
	private float worldRightDirY;
	private float worldRightDirZ;
	private float gForceLateral;
	private float gForceLongitudinal;
	private float gForceVertical;
	private float yaw;
	private float pitch;
	private float roll;

	public CarMotionData() {}

	public int getCarIndex() {
		return carIndex;
	}

	public void setCarIndex(int carIndex) {
		this.carIndex = carIndex;
	}

	public boolean isPlayersCar() {
		return playersCar;
	}

	public void setPlayersCar(boolean playersCar) {
		this.playersCar = playersCar;
	}

	public float getWorldPositionX() {
		return worldPositionX;
	}

	public void setWorldPositionX(float worldPositionX) {
		this.worldPositionX = worldPositionX;
	}

	public float getWorldPositionY() {
		return worldPositionY;
	}

	public void setWorldPositionY(float worldPositionY) {
		this.worldPositionY = worldPositionY;
	}

	public float getWorldPositionZ() {
		return worldPositionZ;
	}

	public void setWorldPositionZ(float worldPositionZ) {
		this.worldPositionZ = worldPositionZ;
	}

	public float getWorldVelocityX() {
		return worldVelocityX;
	}

	public void setWorldVelocityX(float worldVelocityX) {
		this.worldVelocityX = worldVelocityX;
	}

	public float getWorldVelocityY() {
		return worldVelocityY;
	}

	public void setWorldVelocityY(float worldVelocityY) {
		this.worldVelocityY = worldVelocityY;
	}

	public float getWorldVelocityZ() {
		return worldVelocityZ;
	}

	public void setWorldVelocityZ(float worldVelocityZ) {
		this.worldVelocityZ = worldVelocityZ;
	}

	public float getWorldForwardDirX() {
		return worldForwardDirX;
	}

	public void setWorldForwardDirX(float worldForwardDirX) {
		this.worldForwardDirX = worldForwardDirX;
	}

	public float getWorldForwardDirY() {
		return worldForwardDirY;
	}

	public void setWorldForwardDirY(float worldForwardDirY) {
		this.worldForwardDirY = worldForwardDirY;
	}

	public float getWorldForwardDirZ() {
		return worldForwardDirZ;
	}

	public void setWorldForwardDirZ(float worldForwardDirZ) {
		this.worldForwardDirZ = worldForwardDirZ;
	}

	public float getWorldRightDirX() {
		return worldRightDirX;
	}

	public void setWorldRightDirX(float worldRightDirX) {
		this.worldRightDirX = worldRightDirX;
	}

	public float getWorldRightDirY() {
		return worldRightDirY;
	}

	public void setWorldRightDirY(float worldRightDirY) {
		this.worldRightDirY = worldRightDirY;
	}

	public float getWorldRightDirZ() {
		return worldRightDirZ;
	}

	public void setWorldRightDirZ(float worldRightDirZ) {
		this.worldRightDirZ = worldRightDirZ;
	}

	public float getgForceLateral() {
		return gForceLateral;
	}

	public void setgForceLateral(float gForceLateral) {
		this.gForceLateral = gForceLateral;
	}

	public float getgForceLongitudinal() {
		return gForceLongitudinal;
	}

	public void setgForceLongitudinal(float gForceLongitudinal) {
		this.gForceLongitudinal = gForceLongitudinal;
	}

	public float getgForceVertical() {
		return gForceVertical;
	}

	public void setgForceVertical(float gForceVertical) {
		this.gForceVertical = gForceVertical;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getRoll() {
		return roll;
	}

	public void setRoll(float roll) {
		this.roll = roll;
	}

}


// file: PacketCarStatusData.groovy
@groovy.transform.CompileStatic
public class PacketCarStatusData extends Packet {

	private List<CarStatusData> carStatuses;

	public PacketCarStatusData() {}

	public List<CarStatusData> getCarStatuses() {
		return carStatuses;
	}

	public void setCarStatuses(List<CarStatusData> carStatuses) {
		this.carStatuses = carStatuses;
	}

}


// file: PitStatus.groovy
@groovy.transform.CompileStatic
public enum PitStatus {

	NONE,
	PITTING,
	IN_PIT;

	public static PitStatus fromInt(int i) {
		switch(i) {
			case 1:
				return PitStatus.PITTING;
			case 2:
				return PitStatus.IN_PIT;
			default:
				return PitStatus.NONE;
		}
	}
}


// file: ResultStatus.groovy
@groovy.transform.CompileStatic
public enum ResultStatus {

	INVALID,
	INACTIVE,
	ACTIVE,
	FINISHED,
	DISQUALIFIED,
	NOT_CLASSIFIED,
	RETIRED;

	public static ResultStatus fromInt(int i) {
		switch(i) {
			case 0:
				return ResultStatus.INVALID;
			case 1:
				return ResultStatus.INACTIVE;
			case 2:
				return ResultStatus.ACTIVE;
			case 3:
				return ResultStatus.FINISHED;
			case 4:
				return ResultStatus.DISQUALIFIED;
			case 5:
				return ResultStatus.NOT_CLASSIFIED;
			case 6:
				return ResultStatus.RETIRED;
			default:
				return null;
		}
	}
}


// file: ZoneFlag.groovy
@groovy.transform.CompileStatic
public enum ZoneFlag {

	NONE,
	GREEN,
	BLUE,
	YELLOW,
	RED,
	UNKNOWN;

	public static ZoneFlag fromInt(int i) {
		switch(i) {
			case 0:
				return ZoneFlag.NONE;
			case 1:
				return ZoneFlag.GREEN;
			case 2:
				return ZoneFlag.BLUE;
			case 3:
				return ZoneFlag.YELLOW;
			case 4:
				return ZoneFlag.RED;
			default:
				return ZoneFlag.UNKNOWN;
		}
	}
}


// file: SafetyCarStatus.groovy
@groovy.transform.CompileStatic
public enum SafetyCarStatus {

	NO_SAFETY_CAR,
	FULL_SAFETY_CAR,
	VIRTUAL_SAFETY_CAR;

	public static SafetyCarStatus fromInt(int i) {
		switch (i) {
		case 0:
			return SafetyCarStatus.NO_SAFETY_CAR;
		case 1:
			return SafetyCarStatus.FULL_SAFETY_CAR;
		case 2:
			return SafetyCarStatus.VIRTUAL_SAFETY_CAR;
		default:
			return null;
		}
	}
}


// file: PacketCarTelemetryData.groovy
@groovy.transform.CompileStatic
public class PacketCarTelemetryData extends Packet {

	private List<CarTelemetryData> carTelemetryData;
	private ButtonStatus buttonStatus; // TODO, create a representation of this data properly

	public PacketCarTelemetryData() {}

	public List<CarTelemetryData> getCarTelemetryData() {
		return carTelemetryData;
	}
	public void setCarTelemetryData(List<CarTelemetryData> carTelemetryData) {
		this.carTelemetryData = carTelemetryData;
	}

	public ButtonStatus getButtonStatus() {
		return buttonStatus;
	}

	public void setButtonStatus(ButtonStatus buttonStatus) {
		this.buttonStatus = buttonStatus;
	}

}


// file: ButtonStatus.groovy
@groovy.transform.CompileStatic
public class ButtonStatus {

	private boolean crossAPressed;
	private boolean triangleYPressed;
	private boolean circleBPressed;
	private boolean squareXPressed;
	private boolean dpadLeftPressed;
	private boolean dpadRightPressed;
	private boolean dpadUpPressed;
	private boolean dpadDownPressed;
	private boolean optionsMenuPressed;
	private boolean L1LBPressed;
	private boolean R1RBPressed;
	private boolean L2LTPressed;
	private boolean R2RTPressed;
	private boolean leftStickPressed;
	private boolean rightStickPressed;

	public ButtonStatus() {}

	public boolean isCrossAPressed() {
		return crossAPressed;
	}

	public void setCrossAPressed(boolean crossAPressed) {
		this.crossAPressed = crossAPressed;
	}

	public boolean isTriangleYPressed() {
		return triangleYPressed;
	}

	public void setTriangleYPressed(boolean triangleYPressed) {
		this.triangleYPressed = triangleYPressed;
	}

	public boolean isCircleBPressed() {
		return circleBPressed;
	}

	public void setCircleBPressed(boolean circleBPressed) {
		this.circleBPressed = circleBPressed;
	}

	public boolean isSquareXPressed() {
		return squareXPressed;
	}

	public void setSquareXPressed(boolean squareXPressed) {
		this.squareXPressed = squareXPressed;
	}

	public boolean isDpadLeftPressed() {
		return dpadLeftPressed;
	}

	public void setDpadLeftPressed(boolean dpadLeftPressed) {
		this.dpadLeftPressed = dpadLeftPressed;
	}

	public boolean isDpadRightPressed() {
		return dpadRightPressed;
	}

	public void setDpadRightPressed(boolean dpadRightPressed) {
		this.dpadRightPressed = dpadRightPressed;
	}

	public boolean isDpadUpPressed() {
		return dpadUpPressed;
	}

	public void setDpadUpPressed(boolean dpadUpPressed) {
		this.dpadUpPressed = dpadUpPressed;
	}

	public boolean isDpadDownPressed() {
		return dpadDownPressed;
	}

	public void setDpadDownPressed(boolean dpadDownPressed) {
		this.dpadDownPressed = dpadDownPressed;
	}

	public boolean isOptionsMenuPressed() {
		return optionsMenuPressed;
	}

	public void setOptionsMenuPressed(boolean optionsMenuPressed) {
		this.optionsMenuPressed = optionsMenuPressed;
	}

	public boolean isL1LBPressed() {
		return L1LBPressed;
	}

	public void setL1LBPressed(boolean l1lbPressed) {
		L1LBPressed = l1lbPressed;
	}

	public boolean isR1RBPressed() {
		return R1RBPressed;
	}

	public void setR1RBPressed(boolean r1rbPressed) {
		R1RBPressed = r1rbPressed;
	}

	public boolean isL2LTPressed() {
		return L2LTPressed;
	}

	public void setL2LTPressed(boolean l2ltPressed) {
		L2LTPressed = l2ltPressed;
	}

	public boolean isR2RTPressed() {
		return R2RTPressed;
	}

	public void setR2RTPressed(boolean r2rtPressed) {
		R2RTPressed = r2rtPressed;
	}

	public boolean isLeftStickPressed() {
		return leftStickPressed;
	}

	public void setLeftStickPressed(boolean leftStickPressed) {
		this.leftStickPressed = leftStickPressed;
	}

	public boolean isRightStickPressed() {
		return rightStickPressed;
	}

	public void setRightStickPressed(boolean rightStickPressed) {
		this.rightStickPressed = rightStickPressed;
	}

}


// file: PacketBuffer.groovy
/**
 * A wrapper for the byte array to make deserializing the data easier.
 *
 * This class provides a way to traverse the byte array while converting native
 * C data types into Java equivalents. In many cases, the Java data types used
 * are not the most efficient, but were chosen for simplicity (int, for example,
 * is the basis for all non-floating point numbers unless a larger value is
 * needed.)
 *
 *
 * @author eh7n
 *
 */
 @groovy.transform.CompileStatic
public class PacketBuffer {

	private final byte[] ba;
	private int i;

	private PacketBuffer(byte[] array) {
		this.ba = array;
		this.i = 0;
	}

	/**
	 * Wrap the byte array for processing
	 *
	 * @param array
	 * @return the wrapper
	 */
	public static PacketBuffer wrap(byte[] array) {
		return new PacketBuffer(array);
	}

	/**
	 * Gets the next set of bytes based on the input size
	 *
	 * @param size - how many bytes to return
	 * @return the next byte[] based on the current position and the requested size
	 */
	public byte[] getNextBytes(int size) {
		byte[] next = new byte[size];
		int j = 0;
		while (j < size) {
			next[j++] = ba[i++];
		}
		return next;
	}

	/**
	 * Gets and converts the next byte from an unsigned 8bit int to a signed int
	 *
	 * @return the next C uint8 in the byte buffer as a Java int
	 */
	public int getNextUInt8AsInt() {
		return ba[i++] & 0xFF;
	}

	/**
	 * Gets and converts the next byte from an unsigned 8bit int to a boolean
	 *
	 * @return the next C uint8 in the byte buffer as a Java boolean
	 */
	public boolean getNextUInt8AsBoolean() {
		return 1 == (ba[i++] & 0xFF);
	}

	/**
	 * Gets and converts the next two bytes from an unsigned 16bit int to an signed
	 * int
	 *
	 * @return the next C uint16 in the byte buffer as a Java int
	 */
	public int getNextUInt16AsInt() {
		return (ba[i++] & 0xFF) | ((ba[i++] & 0xFF) << 8);
	}

	/**
	 * Gets and converts the next 4 bytes from an unsigned 32bit int to a signed
	 * long
	 *
	 * @return the next C uint in the byte buffer as a Java long
	 */
	public long getNextUIntAsLong() {
		return (ba[i++] & 0xFF) | ((ba[i++] & 0xFF) << 8) | ((ba[i++] & 0xFF) << 16) | ((ba[i++] & 0xFF) << 24);
	}

	/**
	 * Gets the next byte as a 16bit int, no conversion necessary
	 *
	 * @return the next C int8 (byte) as Java int
	 */
	public int getNextInt8AsInt() {
		return ba[i++];
	}

	/**
	 * Gets and converts the next 8 bytes from an unsigned 64bit int to a BigInteger
	 *
	 * @return the next C uint64 in the byte buffer as a Java BigInteger
	 */
	public BigInteger getNextUInt64AsBigInteger() {
		byte[] uint64 = getNextBytes(8);
		return new BigInteger(1, uint64);
	}

	/**
	 * Gets and converts the next 32bits as a float
	 *
	 * @return the next C float as a Java float
	 */
	public float getNextFloat() {
		int floatAsInt = (ba[i++] & 0xFF) | ((ba[i++] & 0xFF) << 8) | ((ba[i++] & 0xFF) << 16)  | ((ba[i++] & 0xFF) << 24);
		return Float.intBitsToFloat(floatAsInt);
	}

	/**
	 * Invokes getNextFloat() against the array n number of times, where n is the
	 * parameter passed in. It will traverse the byte array 4 * n bytes
	 *
	 * @param count the number (n) of floats in the array
	 * @return the next array of C floats as a Java float[]
	 */
	public Float[] getNextFloatArray(int count) {
		Float[] floats = new Float[count];
		for (int k = 0; k < count; k++) {
			floats[k] = getNextFloat();
		}
		return floats;
	}

	/**
	 * Invokes getNextUInt8AsInt() against the array n number of times, where n is
	 * the parameter passed in. It will traverse the byte array n bytes
	 *
	 * @param count the number (n) of ints in the array
	 * @return the next array of C ints as a Java int[]
	 */
	public Integer[] getNextUInt8ArrayAsIntegerArray(int count) {
		Integer[] ints = new Integer[count];
		for (int k = 0; k < count; k++) {
			ints[k] = getNextUInt8AsInt();
		}
		return ints;
	}

	/**
	 * Invokes getNextUInt16AsInt() against the array n number of times, where n is
	 * the parameter passed in. It will traverse the byte array 2 * n bytes
	 *
	 * @param count the number (n) of ints in the array
	 * @return the next array of C ints as a Java int[]
	 */
	public Integer[] getNextUInt16ArrayAsIntegerArray(int count) {
		Integer[] ints = new Integer[count];
		for (int k = 0; k < count; k++) {
			ints[k] = getNextUInt16AsInt();
		}
		return ints;
	}

	/**
	 * Gets and converts the next series of bytes (n) from their unsigned integer
	 * value to a Java char, compiling them into a char array and returning them as
	 * a String. Can also handle null terminated strings, in theory
	 *
	 * @param count the number (n) of chars in the array
	 * @return the next array of C unit8s as a String
	 */
	public String getNextCharArrayAsString(int count) {
		char[] charArr = new char[count];
		boolean reachedEnd = false;
		for (int k = 0; k < count; k++) {
			char curr = (char) ba[i++];
			if(curr == '\u0000') {
				reachedEnd = true;
			}else if (!reachedEnd) {
				charArr[k] = curr;
			}
		}
		return new String(charArr);

	}

	/**
	 * Get the current position of the index in the array
	 *
	 * @return the current position of the buffer
	 */
	public int getCurrentPosition() {
		return i;
	}

	/**
	 * The size of the underlying byte array
	 *
	 * @return the size of the wrapped byte[]
	 */
	public int getSize() {
		return ba.length;
	}

	/**
	 * Returns whether or not there are still more bytes to traverse
	 *
	 * @return true if there is another byte in the array
	 */
	public boolean hasNext() {
		return i < (ba.length - 1);
	}

}


// file: CarStatusData.groovy
@groovy.transform.CompileStatic
public class CarStatusData {

	private int tractionControl;
	private boolean antiLockBrakes;
	private int fuelMix;
	private int frontBrakeBias;
	private boolean pitLimiterOn;
	private float fuelInTank;
	private float fuelCapacity;
	private int maxRpm;
	private int idleRpm;
	private int maxGears;
	private int drsAllowed;
	private WheelData<Integer> tiresWear;
	private int tireCompound;
	private WheelData<Integer> tiresDamage;
	private int frontLeftWheelDamage;
	private int frontRightWingDamage;
	private int rearWingDamage;
	private int engineDamage;
	private int gearBoxDamage;
	private int exhaustDamage;
	private int vehicleFiaFlags;
	private float ersStoreEngery;
	private int ersDeployMode;
	private float ersHarvestedThisLapMGUK;
	private float ersHarvestedThisLapMGUH;
	private float ersDeployedThisLap;


	public CarStatusData() {}

	public int getTractionControl() {
		return tractionControl;
	}

	public void setTractionControl(int tractionControl) {
		this.tractionControl = tractionControl;
	}

	public boolean isAntiLockBrakes() {
		return antiLockBrakes;
	}

	public void setAntiLockBrakes(boolean antiLockBrakes) {
		this.antiLockBrakes = antiLockBrakes;
	}

	public int getFuelMix() {
		return fuelMix;
	}

	public void setFuelMix(int fuelMix) {
		this.fuelMix = fuelMix;
	}

	public int getFrontBrakeBias() {
		return frontBrakeBias;
	}

	public void setFrontBrakeBias(int frontBrakeBias) {
		this.frontBrakeBias = frontBrakeBias;
	}

	public boolean isPitLimiterOn() {
		return pitLimiterOn;
	}

	public void setPitLimiterOn(boolean pitLimiterOn) {
		this.pitLimiterOn = pitLimiterOn;
	}

	public float getFuelInTank() {
		return fuelInTank;
	}

	public void setFuelInTank(float fuelInTank) {
		this.fuelInTank = fuelInTank;
	}

	public float getFuelCapacity() {
		return fuelCapacity;
	}

	public void setFuelCapacity(float fuelCapacity) {
		this.fuelCapacity = fuelCapacity;
	}

	public int getMaxRpm() {
		return maxRpm;
	}

	public void setMaxRpm(int maxRpm) {
		this.maxRpm = maxRpm;
	}

	public int getIdleRpm() {
		return idleRpm;
	}

	public void setIdleRpm(int idleRpm) {
		this.idleRpm = idleRpm;
	}

	public int getMaxGears() {
		return maxGears;
	}

	public void setMaxGears(int maxGears) {
		this.maxGears = maxGears;
	}

	public int getDrsAllowed() {
		return drsAllowed;
	}

	public void setDrsAllowed(int drsAllowed) {
		this.drsAllowed = drsAllowed;
	}

	public WheelData<Integer> getTiresWear() {
		return tiresWear;
	}

	public void setTiresWear(WheelData<Integer> tiresWear) {
		this.tiresWear = tiresWear;
	}

	public int getTireCompound() {
		return tireCompound;
	}

	public void setTireCompound(int tireCompound) {
		this.tireCompound = tireCompound;
	}

	public WheelData<Integer> getTiresDamage() {
		return tiresDamage;
	}

	public void setTiresDamage(WheelData<Integer> tiresDamage) {
		this.tiresDamage = tiresDamage;
	}

	public int getFrontLeftWheelDamage() {
		return frontLeftWheelDamage;
	}

	public void setFrontLeftWheelDamage(int frontLeftWheelDamage) {
		this.frontLeftWheelDamage = frontLeftWheelDamage;
	}

	public int getFrontRightWingDamage() {
		return frontRightWingDamage;
	}

	public void setFrontRightWingDamage(int frontRightWingDamage) {
		this.frontRightWingDamage = frontRightWingDamage;
	}

	public int getRearWingDamage() {
		return rearWingDamage;
	}

	public void setRearWingDamage(int rearWingDamage) {
		this.rearWingDamage = rearWingDamage;
	}

	public int getEngineDamage() {
		return engineDamage;
	}

	public void setEngineDamage(int engineDamage) {
		this.engineDamage = engineDamage;
	}

	public int getGearBoxDamage() {
		return gearBoxDamage;
	}

	public void setGearBoxDamage(int gearBoxDamage) {
		this.gearBoxDamage = gearBoxDamage;
	}

	public int getExhaustDamage() {
		return exhaustDamage;
	}

	public void setExhaustDamage(int exhaustDamage) {
		this.exhaustDamage = exhaustDamage;
	}

	public int getVehicleFiaFlags() {
		return vehicleFiaFlags;
	}

	public void setVehicleFiaFlags(int vehicleFiaFlags) {
		this.vehicleFiaFlags = vehicleFiaFlags;
	}

	public float getErsStoreEngery() {
		return ersStoreEngery;
	}

	public void setErsStoreEngery(float ersStoreEngery) {
		this.ersStoreEngery = ersStoreEngery;
	}

	public int getErsDeployMode() {
		return ersDeployMode;
	}

	public void setErsDeployMode(int ersDeployMode) {
		this.ersDeployMode = ersDeployMode;
	}

	public float getErsHarvestedThisLapMGUK() {
		return ersHarvestedThisLapMGUK;
	}

	public void setErsHarvestedThisLapMGUK(float ersHarvestedThisLapMGUK) {
		this.ersHarvestedThisLapMGUK = ersHarvestedThisLapMGUK;
	}

	public float getErsHarvestedThisLapMGUH() {
		return ersHarvestedThisLapMGUH;
	}

	public void setErsHarvestedThisLapMGUH(float ersHarvestedThisLapMGUH) {
		this.ersHarvestedThisLapMGUH = ersHarvestedThisLapMGUH;
	}

	public float getErsDeployedThisLap() {
		return ersDeployedThisLap;
	}

	public void setErsDeployedThisLap(float ersDeployedThisLap) {
		this.ersDeployedThisLap = ersDeployedThisLap;
	}

}


// file: CarTelemetryData.groovy
@groovy.transform.CompileStatic
public class CarTelemetryData {

	private int speed;
	private int throttle;
	private int steer;
	private int brake;
	private int clutch;
	private int gear;
	private int engineRpm;
	private boolean drs;
	private int revLightsPercent;
	private WheelData<Integer> brakeTemperature;
	private WheelData<Integer> tireSurfaceTemperature;
	private WheelData<Integer> tireInnerTemperature;
	private int engineTemperature;
	private WheelData<Float> tirePressure;

	public CarTelemetryData() {}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getThrottle() {
		return throttle;
	}

	public void setThrottle(int throttle) {
		this.throttle = throttle;
	}

	public int getSteer() {
		return steer;
	}

	public void setSteer(int steer) {
		this.steer = steer;
	}

	public int getBrake() {
		return brake;
	}

	public void setBrake(int brake) {
		this.brake = brake;
	}

	public int getClutch() {
		return clutch;
	}

	public void setClutch(int clutch) {
		this.clutch = clutch;
	}

	public int getGear() {
		return gear;
	}

	public void setGear(int gear) {
		this.gear = gear;
	}

	public int getEngineRpm() {
		return engineRpm;
	}

	public void setEngineRpm(int engineRpm) {
		this.engineRpm = engineRpm;
	}

	public boolean isDrs() {
		return drs;
	}

	public void setDrs(boolean drs) {
		this.drs = drs;
	}

	public int getRevLightsPercent() {
		return revLightsPercent;
	}

	public void setRevLightsPercent(int revLightsPercent) {
		this.revLightsPercent = revLightsPercent;
	}

	public WheelData<Integer> getBrakeTemperature() {
		return brakeTemperature;
	}

	public void setBrakeTemperature(WheelData<Integer> brakeTemperature) {
		this.brakeTemperature = brakeTemperature;
	}

	public WheelData<Integer> getTireSurfaceTemperature() {
		return tireSurfaceTemperature;
	}

	public void setTireSurfaceTemperature(WheelData<Integer> tireSurfaceTemperature) {
		this.tireSurfaceTemperature = tireSurfaceTemperature;
	}

	public WheelData<Integer> getTireInnerTemperature() {
		return tireInnerTemperature;
	}

	public void setTireInnerTemperature(WheelData<Integer> tireInnerTemperature) {
		this.tireInnerTemperature = tireInnerTemperature;
	}

	public int getEngineTemperature() {
		return engineTemperature;
	}

	public void setEngineTemperature(int engineTemperature) {
		this.engineTemperature = engineTemperature;
	}

	public WheelData<Float> getTirePressure() {
		return tirePressure;
	}

	public void setTirePressure(WheelData<Float> tirePressure) {
		this.tirePressure = tirePressure;
	}

}


// file: MarshalZone.groovy
@groovy.transform.CompileStatic
public class MarshalZone {

	private float zoneStart;
	private ZoneFlag zoneFlag;

	public float getZoneStart() {
		return zoneStart;
	}

	public void setZoneStart(float zoneStart) {
		this.zoneStart = zoneStart;
	}

	public ZoneFlag getZoneFlag() {
		return zoneFlag;
	}

	public void setZoneFlag(ZoneFlag zoneFlag) {
		this.zoneFlag = zoneFlag;
	}

}


// file: Packet.groovy
@groovy.transform.CompileStatic
public abstract class Packet {

	private Header header;

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public String toJSON() {
		ObjectMapper mapper = new ObjectMapper();
		String json = "";
		try {
			json = mapper.writeValueAsString(this);
		}catch(Exception e) {
			//TODO: Handle this exception
		}
		return json.replace("\\u0000", "");
	}

}


// file: PacketSessionData.groovy
@groovy.transform.CompileStatic
public class PacketSessionData extends Packet {

	private Weather weather;
	private int trackTemperature;
	private int airTemperature;
	private int totalLaps;
	private int trackLength;
	private SessionType sessionType;
	private int trackId;
	private Era era;
	private int sessionTimeLeft;
	private int sessionDuration;
	private int pitSpeedLimit;
	private boolean gamePaused;
	private boolean spectating;
	private int spectatorCarIndex;
	private boolean sliProNativeSupport;
	private int numMarshalZones;
	private List<MarshalZone> marshalZones;
	private SafetyCarStatus safetyCarStatus;
	private boolean networkGame;

	public PacketSessionData() {}

	public Weather getWeather() {
		return weather;
	}

	public void setWeather(Weather weather) {
		this.weather = weather;
	}

	public int getTrackTemperature() {
		return trackTemperature;
	}

	public void setTrackTemperature(int trackTemperature) {
		this.trackTemperature = trackTemperature;
	}

	public int getAirTemperature() {
		return airTemperature;
	}

	public void setAirTemperature(int airTemperature) {
		this.airTemperature = airTemperature;
	}

	public int getTotalLaps() {
		return totalLaps;
	}

	public void setTotalLaps(int totalLaps) {
		this.totalLaps = totalLaps;
	}

	public int getTrackLength() {
		return trackLength;
	}

	public void setTrackLength(int trackLength) {
		this.trackLength = trackLength;
	}

	public SessionType getSessionType() {
		return sessionType;
	}

	public void setSessionType(SessionType sessionType) {
		this.sessionType = sessionType;
	}

	public int getTrackId() {
		return trackId;
	}

	public void setTrackId(int trackId) {
		this.trackId = trackId;
	}

	public Era getEra() {
		return era;
	}

	public void setEra(Era era) {
		this.era = era;
	}

	public int getSessionTimeLeft() {
		return sessionTimeLeft;
	}

	public void setSessionTimeLeft(int sessionTimeLeft) {
		this.sessionTimeLeft = sessionTimeLeft;
	}

	public int getSessionDuration() {
		return sessionDuration;
	}

	public void setSessionDuration(int sessionDuration) {
		this.sessionDuration = sessionDuration;
	}

	public int getPitSpeedLimit() {
		return pitSpeedLimit;
	}

	public void setPitSpeedLimit(int pitSpeedLimit) {
		this.pitSpeedLimit = pitSpeedLimit;
	}

	public boolean isGamePaused() {
		return gamePaused;
	}

	public void setGamePaused(boolean gamePaused) {
		this.gamePaused = gamePaused;
	}

	public boolean isSpectating() {
		return spectating;
	}

	public void setSpectating(boolean spectating) {
		this.spectating = spectating;
	}

	public int getSpectatorCarIndex() {
		return spectatorCarIndex;
	}

	public void setSpectatorCarIndex(int spectatorCarIndex) {
		this.spectatorCarIndex = spectatorCarIndex;
	}

	public boolean isSliProNativeSupport() {
		return sliProNativeSupport;
	}

	public void setSliProNativeSupport(boolean sliProNativeSupport) {
		this.sliProNativeSupport = sliProNativeSupport;
	}

	public int getNumMarshalZones() {
		return numMarshalZones;
	}

	public void setNumMarshalZones(int numMarshalZones) {
		this.numMarshalZones = numMarshalZones;
	}

	public List<MarshalZone> getMarshalZones() {
		return marshalZones;
	}

	public void setMarshalZones(List<MarshalZone> marshalZones) {
		this.marshalZones = marshalZones;
	}

	public SafetyCarStatus getSafetyCarStatus() {
		return safetyCarStatus;
	}

	public void setSafetyCarStatus(SafetyCarStatus safetyCarStatus) {
		this.safetyCarStatus = safetyCarStatus;
	}

	public boolean isNetworkGame() {
		return networkGame;
	}

	public void setNetworkGame(boolean networkGame) {
		this.networkGame = networkGame;
	}

}


// file: LapData.groovy
@groovy.transform.CompileStatic
public class LapData {

	private float carIndex;
	private boolean playersCar;
	private float lastLapTime;
	private float currentLapTime;
	private float bestLaptTime;
	private float sector1Time;
	private float sector2Time;
	private float lapDistance;
	private float totalDistance;
	private float safetyCarDelta;
	private int carPosition;
	private int currentLapNum;
	private PitStatus pitStatus;
	private int sector;
	private boolean currentLapInvalid;
	private int penalties;
	private int gridPosition;
	private DriverStatus driverStatus;
	private ResultStatus resultStatus;

	public LapData() {}

	public float getCarIndex() {
		return carIndex;
	}
	public void setCarIndex(float carIndex) {
		this.carIndex = carIndex;
	}
	public boolean isPlayersCar() {
		return playersCar;
	}
	public void setPlayersCar(boolean playersCar) {
		this.playersCar = playersCar;
	}
	public float getLastLapTime() {
		return lastLapTime;
	}
	public void setLastLapTime(float lastLapTime) {
		this.lastLapTime = lastLapTime;
	}
	public float getCurrentLapTime() {
		return currentLapTime;
	}
	public void setCurrentLapTime(float currentLapTime) {
		this.currentLapTime = currentLapTime;
	}
	public float getBestLaptTime() {
		return bestLaptTime;
	}
	public void setBestLaptTime(float bestLaptTime) {
		this.bestLaptTime = bestLaptTime;
	}
	public float getSector1Time() {
		return sector1Time;
	}
	public void setSector1Time(float sector1Time) {
		this.sector1Time = sector1Time;
	}
	public float getSector2Time() {
		return sector2Time;
	}
	public void setSector2Time(float sector2Time) {
		this.sector2Time = sector2Time;
	}
	public float getLapDistance() {
		return lapDistance;
	}
	public void setLapDistance(float lapDistance) {
		this.lapDistance = lapDistance;
	}
	public float getTotalDistance() {
		return totalDistance;
	}
	public void setTotalDistance(float totalDistance) {
		this.totalDistance = totalDistance;
	}
	public float getSafetyCarDelta() {
		return safetyCarDelta;
	}
	public void setSafetyCarDelta(float safetyCarDelta) {
		this.safetyCarDelta = safetyCarDelta;
	}
	public int getCarPosition() {
		return carPosition;
	}
	public void setCarPosition(int carPosition) {
		this.carPosition = carPosition;
	}
	public int getCurrentLapNum() {
		return currentLapNum;
	}
	public void setCurrentLapNum(int currentLapNum) {
		this.currentLapNum = currentLapNum;
	}
	public PitStatus getPitStatus() {
		return pitStatus;
	}
	public void setPitStatus(PitStatus pitStatus) {
		this.pitStatus = pitStatus;
	}
	public int getSector() {
		return sector;
	}
	public void setSector(int sector) {
		this.sector = sector;
	}
	public boolean isCurrentLapInvalid() {
		return currentLapInvalid;
	}
	public void setCurrentLapInvalid(boolean currentLapInvalid) {
		this.currentLapInvalid = currentLapInvalid;
	}
	public int getPenalties() {
		return penalties;
	}
	public void setPenalties(int penalties) {
		this.penalties = penalties;
	}
	public int getGridPosition() {
		return gridPosition;
	}
	public void setGridPosition(int gridPosition) {
		this.gridPosition = gridPosition;
	}
	public DriverStatus getDriverStatus() {
		return driverStatus;
	}
	public void setDriverStatus(DriverStatus driverStatus) {
		this.driverStatus = driverStatus;
	}
	public ResultStatus getResultStatus() {
		return resultStatus;
	}
	public void setResultStatus(ResultStatus resultStatus) {
		this.resultStatus = resultStatus;
	}

}


// file: ParticipantData.groovy
@groovy.transform.CompileStatic
public class ParticipantData {

	private boolean aiControlled;
	private int driverId;
	private int teamId;
	private int raceNumber;
	private int nationality;
	private String name;

	public ParticipantData() {}

	public boolean isAiControlled() {
		return aiControlled;
	}
	public void setAiControlled(boolean aiControlled) {
		this.aiControlled = aiControlled;
	}
	public int getDriverId() {
		return driverId;
	}
	public void setDriverId(int driverId) {
		this.driverId = driverId;
	}
	public int getTeamId() {
		return teamId;
	}
	public void setTeamId(int teamId) {
		this.teamId = teamId;
	}
	public int getRaceNumber() {
		return raceNumber;
	}
	public void setRaceNumber(int raceNumber) {
		this.raceNumber = raceNumber;
	}
	public int getNationality() {
		return nationality;
	}
	public void setNationality(int nationality) {
		this.nationality = nationality;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}


// file: PacketCarSetupData.groovy
@groovy.transform.CompileStatic
public class PacketCarSetupData extends Packet {

	private List<CarSetupData> carSetups;

	public PacketCarSetupData() {}

	public List<CarSetupData> getCarSetups() {
		return carSetups;
	}
	public void setCarSetups(List<CarSetupData> carSetups) {
		this.carSetups = carSetups;
	}

}


// file: SessionType.groovy
@groovy.transform.CompileStatic
public enum SessionType {

	UNKNOWN,
	P1,
	P2,
	P3,
	SHORT_P,
	Q1,
	Q2,
	Q3,
	SHORT_Q,
	OSQ,
	R,
	R2,
	TIME_TRIAL;

	public static SessionType fromInt(int i) {
		switch(i) {
			case 0:
				return SessionType.UNKNOWN;
			case 1:
				return SessionType.P1;
			case 2:
				return SessionType.P2;
			case 3:
				return SessionType.P3;
			case 4:
				return SessionType.SHORT_P;
			case 5:
				return SessionType.Q1;
			case 6:
				return SessionType.Q2;
			case 7:
				return SessionType.Q3;
			case 8:
				return SessionType.SHORT_Q;
			case 9:
				return SessionType.OSQ;
			case 10:
				return SessionType.R;
			case 11:
				return SessionType.R2;
			case 12:
				return SessionType.TIME_TRIAL;
			default:
				return null;
		}
	}

}


// file: Header.groovy
@groovy.transform.CompileStatic
public class Header {

	private int packetFormat;
	private long packetVersion;
	private int packetId;
	private BigInteger sessionUID;
	private float sessionTime;
	private long frameIdentifier;
	private int playerCarIndex;

	public Header() {}

	public int getPacketFormat() {
		return packetFormat;
	}

	public void setPacketFormat(int packetFormat) {
		this.packetFormat = packetFormat;
	}

	public long getPacketVersion() {
		return packetVersion;
	}

	public void setPacketVersion(long packetVersion) {
		this.packetVersion = packetVersion;
	}

	public int getPacketId() {
		return packetId;
	}

	public void setPacketId(int packetId) {
		this.packetId = packetId;
	}

	public BigInteger getSessionUID() {
		return sessionUID;
	}

	public void setSessionUID(BigInteger sessionUID) {
		this.sessionUID = sessionUID;
	}

	public float getSessionTime() {
		return sessionTime;
	}

	public void setSessionTime(float sessionTime) {
		this.sessionTime = sessionTime;
	}

	public long getFrameIdentifier() {
		return frameIdentifier;
	}

	public void setFrameIdentifier(long frameIdentifier) {
		this.frameIdentifier = frameIdentifier;
	}

	public int getPlayerCarIndex() {
		return playerCarIndex;
	}

	public void setPlayerCarIndex(int playerCarIndex) {
		this.playerCarIndex = playerCarIndex;
	}

	@Override
	public String toString() {
		return "Header :: packetFormat:" + this.getPacketFormat() +
		", version:" + this.getPacketVersion() +
		", packetId:" + this.getPacketId() +
		", sessionUID:" + this.getSessionUID() +
		", sessionTime:" + this.getSessionTime() +
		", frameIdentifier:" + this.getFrameIdentifier() +
		", playerCarIndex:" + this.getPlayerCarIndex();
	}
}


// file: PacketLapData.groovy
@groovy.transform.CompileStatic
public class PacketLapData extends Packet {

	private List<LapData> lapDataList;

	public PacketLapData() {}

	public List<LapData> getLapDataList() {
		return lapDataList;
	}

	public void setLapDataList(List<LapData> lapDataList) {
		this.lapDataList = lapDataList;
	}

}


// file: PacketMotionData.groovy
@groovy.transform.CompileStatic
public class PacketMotionData extends Packet {

	private List<CarMotionData> carMotionDataList;
	private WheelData<Float> suspensionPosition;
	private WheelData<Float> suspensionVelocity;
	private WheelData<Float> suspensionAcceleration;
	private WheelData<Float> wheelSpeed;
	private WheelData<Float> wheelSlip;
	private float localVelocityX;
	private float localVelocityY;
	private float localVelocityZ;
	private float angularVelocityX;
	private float angularVelocityY;
	private float angularVelocityZ;
	private float angularAccelerationX;
	private float angularAccelerationY;
	private float angularAccelerationZ;
	private float frontWheelsAngle;

	public PacketMotionData() {}

	public List<CarMotionData> getCarMotionDataList() {
		return carMotionDataList;
	}

	public void setCarMotionDataList(List<CarMotionData> carMotionDataList) {
		this.carMotionDataList = carMotionDataList;
	}

	public WheelData<Float> getSuspensionPosition() {
		return suspensionPosition;
	}

	public void setSuspensionPosition(WheelData<Float> suspensionPosition) {
		this.suspensionPosition = suspensionPosition;
	}

	public WheelData<Float> getSuspensionVelocity() {
		return suspensionVelocity;
	}

	public void setSuspensionVelocity(WheelData<Float> suspensionVelocity) {
		this.suspensionVelocity = suspensionVelocity;
	}

	public WheelData<Float> getSuspensionAcceleration() {
		return suspensionAcceleration;
	}

	public void setSuspensionAcceleration(WheelData<Float> suspensionAcceleration) {
		this.suspensionAcceleration = suspensionAcceleration;
	}

	public WheelData<Float> getWheelSpeed() {
		return wheelSpeed;
	}

	public void setWheelSpeed(WheelData<Float> wheelSpeed) {
		this.wheelSpeed = wheelSpeed;
	}

	public WheelData<Float> getWheelSlip() {
		return wheelSlip;
	}

	public void setWheelSlip(WheelData<Float> wheelSlip) {
		this.wheelSlip = wheelSlip;
	}

	public float getLocalVelocityX() {
		return localVelocityX;
	}

	public void setLocalVelocityX(float localVelocityX) {
		this.localVelocityX = localVelocityX;
	}

	public float getLocalVelocityY() {
		return localVelocityY;
	}

	public void setLocalVelocityY(float localVelocityY) {
		this.localVelocityY = localVelocityY;
	}

	public float getLocalVelocityZ() {
		return localVelocityZ;
	}

	public void setLocalVelocityZ(float localVelocityZ) {
		this.localVelocityZ = localVelocityZ;
	}

	public float getAngularVelocityX() {
		return angularVelocityX;
	}

	public void setAngularVelocityX(float angularVelocityX) {
		this.angularVelocityX = angularVelocityX;
	}

	public float getAngularVelocityY() {
		return angularVelocityY;
	}

	public void setAngularVelocityY(float angularVelocityY) {
		this.angularVelocityY = angularVelocityY;
	}

	public float getAngularVelocityZ() {
		return angularVelocityZ;
	}

	public void setAngularVelocityZ(float angularVelocityZ) {
		this.angularVelocityZ = angularVelocityZ;
	}

	public float getAngularAccelerationX() {
		return angularAccelerationX;
	}

	public void setAngularAccelerationX(float angularAccelerationX) {
		this.angularAccelerationX = angularAccelerationX;
	}

	public float getAngularAccelerationY() {
		return angularAccelerationY;
	}

	public void setAngularAccelerationY(float angularAccelerationY) {
		this.angularAccelerationY = angularAccelerationY;
	}

	public float getAngularAccelerationZ() {
		return angularAccelerationZ;
	}

	public void setAngularAccelerationZ(float angularAccelerationZ) {
		this.angularAccelerationZ = angularAccelerationZ;
	}

	public float getFrontWheelsAngle() {
		return frontWheelsAngle;
	}

	public void setFrontWheelsAngle(float frontWheelsAngle) {
		this.frontWheelsAngle = frontWheelsAngle;
	}

}


// file: PacketEventData.groovy
@groovy.transform.CompileStatic
public class PacketEventData extends Packet {

	private String eventCode;

	public PacketEventData() {}

	public String getEventCode() {
		return eventCode;
	}

	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

}


// file: Weather.groovy
@groovy.transform.CompileStatic
public enum Weather {

	CLEAR,
	LIGHT_CLOUD,
	OVERCAST,
	LIGHT_RAIN,
	HEAVY_RAIN,
	STORM;

	public static Weather fromInt(int i) {
		switch (i) {
		case 0:
			return Weather.CLEAR;
		case 1:
			return Weather.LIGHT_CLOUD;
		case 2:
			return Weather.OVERCAST;
		case 3:
			return Weather.LIGHT_RAIN;
		case 4:
			return Weather.HEAVY_RAIN;
		default:
			return Weather.STORM;
		}
	}
}


// file: CarSetupData.groovy
@groovy.transform.CompileStatic
public class CarSetupData {

	private int frontWing;
	private int rearWing;
	private int onThrottle;
	private int offThrottle;
	private float frontCamber;
	private float rearCamber;
	private float frontToe;
	private float rearToe;
	private int frontSuspension;
	private int rearSuspension;
	private int frontAntiRollBar;
	private int rearAntiRollBar;
	private int frontSuspensionHeight;
	private int rearSuspensionHeight;
	private int brakePressure;
	private int brakeBias;
	private float frontTirePressure;
	private float rearTirePressure;
	private int ballast;
	private float fuelLoad;

	public CarSetupData() {
	}

	public int getFrontWing() {
		return frontWing;
	}

	public void setFrontWing(int frontWing) {
		this.frontWing = frontWing;
	}

	public int getRearWing() {
		return rearWing;
	}

	public void setRearWing(int rearWing) {
		this.rearWing = rearWing;
	}

	public int getOnThrottle() {
		return onThrottle;
	}

	public void setOnThrottle(int onThrottle) {
		this.onThrottle = onThrottle;
	}

	public int getOffThrottle() {
		return offThrottle;
	}

	public void setOffThrottle(int offThrottle) {
		this.offThrottle = offThrottle;
	}

	public float getFrontCamber() {
		return frontCamber;
	}

	public void setFrontCamber(float frontCamber) {
		this.frontCamber = frontCamber;
	}

	public float getRearCamber() {
		return rearCamber;
	}

	public void setRearCamber(float rearCamber) {
		this.rearCamber = rearCamber;
	}

	public float getFrontToe() {
		return frontToe;
	}

	public void setFrontToe(float frontToe) {
		this.frontToe = frontToe;
	}

	public float getRearToe() {
		return rearToe;
	}

	public void setRearToe(float rearToe) {
		this.rearToe = rearToe;
	}

	public int getFrontSuspension() {
		return frontSuspension;
	}

	public void setFrontSuspension(int frontSuspension) {
		this.frontSuspension = frontSuspension;
	}

	public int getRearSuspension() {
		return rearSuspension;
	}

	public void setRearSuspension(int rearSuspension) {
		this.rearSuspension = rearSuspension;
	}

	public int getFrontAntiRollBar() {
		return frontAntiRollBar;
	}

	public void setFrontAntiRollBar(int frontAntiRollBar) {
		this.frontAntiRollBar = frontAntiRollBar;
	}

	public int getRearAntiRollBar() {
		return rearAntiRollBar;
	}

	public void setRearAntiRollBar(int rearAntiRollBar) {
		this.rearAntiRollBar = rearAntiRollBar;
	}

	public int getFrontSuspensionHeight() {
		return frontSuspensionHeight;
	}

	public void setFrontSuspensionHeight(int frontSuspensionHeight) {
		this.frontSuspensionHeight = frontSuspensionHeight;
	}

	public int getRearSuspensionHeight() {
		return rearSuspensionHeight;
	}

	public void setRearSuspensionHeight(int rearSuspensionHeight) {
		this.rearSuspensionHeight = rearSuspensionHeight;
	}

	public int getBrakePressure() {
		return brakePressure;
	}

	public void setBrakePressure(int brakePressure) {
		this.brakePressure = brakePressure;
	}

	public int getBrakeBias() {
		return brakeBias;
	}

	public void setBrakeBias(int brakeBias) {
		this.brakeBias = brakeBias;
	}

	public float getFrontTirePressure() {
		return frontTirePressure;
	}

	public void setFrontTirePressure(float frontTirePressure) {
		this.frontTirePressure = frontTirePressure;
	}

	public float getRearTirePressure() {
		return rearTirePressure;
	}

	public void setRearTirePressure(float rearTirePressure) {
		this.rearTirePressure = rearTirePressure;
	}

	public int getBallast() {
		return ballast;
	}

	public void setBallast(int ballast) {
		this.ballast = ballast;
	}

	public float getFuelLoad() {
		return fuelLoad;
	}

	public void setFuelLoad(float fuelLoad) {
		this.fuelLoad = fuelLoad;
	}

}
