package laas.openrobots.ontology.events;

public interface IWatcher {

	public abstract String getWatchQuery();

	public abstract void notifySubscriber();

}