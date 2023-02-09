package dakota.dude.handler.interaction.model;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * A wrapper for HashMap<Long, T> which implements a 1 minute minimum, 15 minute maximum cache.
 * Proper usage is restricted to one use of put(), followed by any number of uses of get().
 * The initial call to put() starts a 1 minute timer and a 15 minute timer, after which the value will be removed.
 * Each successive call to get() refreshes the 1 minute timer, but the value is always removed after the 15 minute timer if it is reached.
 * 
 */
public class CacheMap<T> {
	
	private static final Logger logger = LogManager.getLogger();

	private Map<Long, Data> values = new HashMap<>();
	
	private class Data {
		public T value;
		public Disposable removal;
		public Disposable finalRemoval;
		
		public Data(T value, Disposable removal, Disposable finalRemoval) {
			this.value = value;
			this.removal = removal;
			this.finalRemoval = finalRemoval;
		}
	}
	
	/**
	 * Adds a value into the map, along with two routines that will remove it, one after 1 minute and one after 15 minutes.
	 * The 1 minute routine will be refreshed on each use of get(), but the 15 minute routine cannot be canceled.
	 * @param key
	 * @param value
	 */
	public void put(Long key, T value) {
		//ensure a maximum of 15 minutes for this value
		Data data = new Data(value,
				Mono.delay(Duration.ofMinutes(1)).doOnNext($ -> remove(key)).subscribe(),
				Mono.delay(Duration.ofMinutes(15)).doOnNext($ -> finalRemove(key)).subscribe());
		values.put(key, data);
	}
	
	/**
	 * Cancels the 15 minute routine if we reach the one minute routine.
	 * @param key
	 */
	private void remove(Long key) {
		Data data = values.get(key);
		if(data != null) {
			synchronized(data) {
				data.finalRemoval.dispose();
			}
		}
		values.remove(key);
		logger.debug("Removing data of key " + key + " after 15 minutes");
	}
	
	/**
	 * Cancels the 1 minute routine if we hit the 15 minute routine.
	 * @param key
	 */
	private void finalRemove(Long key) {
		Data data = values.get(key);
		if(data != null) {
			synchronized(data) {
				data.removal.dispose();
			}
		}
		values.remove(key);
		logger.debug("Removing data of key " + key + " after 15 minutes");
	}
	
	/**
	 * Retrieves the value in the cache, if present.
	 * If a value was present, its 1 minute removal timer is refreshed.
	 * @param key
	 * @return
	 */
	public T get(Long key) {
		Data data = values.get(key);
		//refresh the 1 minute delete; remove the old delete routine, and add another for 1 minute from now
		if(data != null) {
			synchronized(data) {
				data.removal.dispose();
				data.removal = Mono.delay(Duration.ofMinutes(1)).doOnNext($ -> remove(key)).subscribe();
				return data.value;
			}
		}
		return null;
	}
}
