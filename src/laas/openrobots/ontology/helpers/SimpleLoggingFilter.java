package laas.openrobots.ontology.helpers;

public class SimpleLoggingFilter implements ILoggingFilter {

	@Override
	public String filter(String msg) {
		if (msg.contains("Adding")) {
			return Logger.Colors.GREEN.format(msg);
		}
		else if (msg.contains("Removing")) {
			return Logger.Colors.RED.format(msg);
		}
		else if (msg.contains("Updating")) {
			return Logger.Colors.YELLOW.format(msg);
		}
		else if (msg.contains("Event") || msg.contains("event")) {
			return Logger.Colors.PURPLE.format(msg);
		}
		else return msg;
	}

}
