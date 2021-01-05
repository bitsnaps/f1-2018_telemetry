

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
