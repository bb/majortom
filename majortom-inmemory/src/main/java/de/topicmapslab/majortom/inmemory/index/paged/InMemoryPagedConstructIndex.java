/*******************************************************************************
 * Copyright 2010, Topic Map Lab ( http://www.topicmapslab.de )
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.topicmapslab.majortom.inmemory.index.paged;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.tmapi.core.Association;
import org.tmapi.core.Construct;
import org.tmapi.core.Name;
import org.tmapi.core.Occurrence;
import org.tmapi.core.Role;
import org.tmapi.core.TMAPIRuntimeException;
import org.tmapi.core.Topic;
import org.tmapi.core.Variant;

import de.topicmapslab.majortom.inmemory.index.InMemoryIndex;
import de.topicmapslab.majortom.inmemory.store.InMemoryTopicMapStore;
import de.topicmapslab.majortom.model.event.ITopicMapListener;
import de.topicmapslab.majortom.model.event.TopicMapEventType;
import de.topicmapslab.majortom.model.exception.IndexException;
import de.topicmapslab.majortom.model.index.paging.IPagedConstructIndex;
import de.topicmapslab.majortom.util.HashUtil;

/**
 * Implementation of {@link IPagedConstructIndex}
 * 
 * @author Sven Krosse
 * 
 */
public class InMemoryPagedConstructIndex extends InMemoryIndex implements IPagedConstructIndex, ITopicMapListener {

	/**
	 * enumeration of map keys
	 */
	enum Key {
		TYPES,

		SUPERTYPE,

		NAMES,

		OCCURRENCES,

		VARIANTS,

		ROLES,

		ASSOCIATIONS_PLAYED,

		ROLES_PLAYED
	}

	private Map<Key, Map<Construct, List<? extends Construct>>> cachedConstructs;
	private Map<Key, Map<Construct, Map<Comparator<? extends Construct>, List<? extends Construct>>>> cachedComparedConstructs;

	/**
	 * @param store
	 */
	public InMemoryPagedConstructIndex(InMemoryTopicMapStore store) {
		super(store);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Association> getAssociationsPlayed(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ASSOCIATIONS_PLAYED, "getAssociationsPlayed", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Association> getAssociationsPlayed(Topic topic, int offset, int limit, Comparator<Association> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ASSOCIATIONS_PLAYED, "getAssociationsPlayed", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfAssociationsPlayed(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ASSOCIATIONS_PLAYED, "getAssociationsPlayed", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Name> getNames(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.NAMES, "getNames", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Name> getNames(Topic topic, int offset, int limit, Comparator<Name> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.NAMES, "getNames", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfNames(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.NAMES, "getNames", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Occurrence> getOccurrences(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.OCCURRENCES, "getOccurrences", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Occurrence> getOccurrences(Topic topic, int offset, int limit, Comparator<Occurrence> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.OCCURRENCES, "getOccurrences", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfOccurrences(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.OCCURRENCES, "getOccurrences", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Role> getRoles(Association association, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES, "getRoles", association, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Role> getRoles(Association association, int offset, int limit, Comparator<Role> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES, "getRoles", association, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfRoles(Association association) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES, "getRoles", association).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Role> getRolesPlayed(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES_PLAYED, "getRolesPlayed", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Role> getRolesPlayed(Topic topic, int offset, int limit, Comparator<Role> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES_PLAYED, "getRolesPlayed", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfRolesPlayed(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.ROLES_PLAYED, "getRolesPlayed", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Topic> getSupertypes(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.SUPERTYPE, "getSupertypes", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Topic> getSupertypes(Topic topic, int offset, int limit, Comparator<Topic> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.SUPERTYPE, "getSupertypes", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfSupertypes(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.SUPERTYPE, "getSupertypes", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Topic> getTypes(Topic topic, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.TYPES, "getTypes", topic, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Topic> getTypes(Topic topic, int offset, int limit, Comparator<Topic> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.TYPES, "getTypes", topic, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfTypes(Topic topic) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.TYPES, "getTypes", topic).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Variant> getVariants(Name name, int offset, int limit) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.VARIANTS, "getVariants", name, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Variant> getVariants(Name name, int offset, int limit, Comparator<Variant> comparator) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.VARIANTS, "getVariants", name, offset, limit, comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNumberOfVariants(Name name) {
		if (!isOpen()) {
			throw new TMAPIRuntimeException("Index is closed!");
		}
		return getConstructs(Key.VARIANTS, "getVariants", name).size();
	}

	/**
	 * Internal method to read dependent constructs of the given parent
	 * construct.
	 * 
	 * @param <T>
	 *            the type of dependent constructs
	 * @param key
	 *            the key of dependent constructs
	 * @param methodName
	 *            the method name which will be called if values are missing
	 * @param parent
	 *            the parent construct
	 * @return dependent constructs of the given parent construct
	 */
	@SuppressWarnings("unchecked")
	private final <T extends Construct> List<T> getConstructs(Key key, final String methodName, Construct parent) {
		/*
		 * initialize cache
		 */
		if (cachedConstructs == null) {
			cachedConstructs = HashUtil.getHashMap();
		}
		/*
		 * get mapping of cached constructs
		 */
		Map<Construct, List<? extends Construct>> map = cachedConstructs.get(key);
		if (map == null) {
			map = HashUtil.getWeakHashMap();
			cachedConstructs.put(key, map);
		}
		/*
		 * get constructs
		 */
		List<T> list = (List<T>) map.get(parent);
		if (list == null) {
			try {
				/*
				 * call method to get constructs
				 */
				Method method = parent.getClass().getMethod(methodName);
				list = HashUtil.getList((Collection<T>) method.invoke(parent));
				map.put(parent, list);
			} catch (SecurityException e) {
				throw new IndexException(e);
			} catch (NoSuchMethodException e) {
				throw new IndexException(e);
			} catch (IllegalArgumentException e) {
				throw new IndexException(e);
			} catch (IllegalAccessException e) {
				throw new IndexException(e);
			} catch (InvocationTargetException e) {
				throw new IndexException(e);
			}
		}
		return list;
	}

	/**
	 * Internal method to read dependent constructs of the given parent
	 * construct within the given range.
	 * 
	 * @param <T>
	 *            the type of dependent constructs
	 * @param key
	 *            the key of dependent constructs
	 * @param methodName
	 *            the method name which will be called if values are missing
	 * @param parent
	 *            the parent construct
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @return dependent constructs of the given parent construct within the
	 *         given range
	 */
	private final <T extends Construct> List<T> getConstructs(Key key, final String methodName, Construct parent, int offset, int limit) {
		List<T> list = getConstructs(key, methodName, parent);
		return secureSubList(list, offset, limit);
	}

	/**
	 * Internal method to read dependent constructs of the given parent
	 * construct.
	 * 
	 * @param <T>
	 *            the type of dependent constructs
	 * @param key
	 *            the key of dependent constructs
	 * @param methodName
	 *            the method name which will be called if values are missing
	 * @param parent
	 *            the parent construct
	 * @param comparator
	 *            the comparators
	 * @return dependent constructs of the given parent construct
	 */
	@SuppressWarnings("unchecked")
	private final <T extends Construct> List<T> getConstructs(Key key, final String methodName, Construct parent, Comparator<T> comparator) {
		/*
		 * initialize cache
		 */
		if (cachedComparedConstructs == null) {
			cachedComparedConstructs = HashUtil.getHashMap();
		}
		/*
		 * get mapping of parent to cached compared constructs by key
		 */
		Map<Construct, Map<Comparator<? extends Construct>, List<? extends Construct>>> compared = cachedComparedConstructs.get(key);
		if (compared == null) {
			compared = HashUtil.getWeakHashMap();
			cachedComparedConstructs.put(key, compared);
		}
		/*
		 * get mapping of cached compared constructs by parent
		 */
		Map<Comparator<? extends Construct>, List<? extends Construct>> map = compared.get(parent);
		if (map == null) {
			map = HashUtil.getWeakHashMap();
			compared.put(parent, map);
		}
		/*
		 * get constructs by comparator
		 */
		List<T> list = (List<T>) map.get(comparator);
		if (list == null) {
			try {
				/*
				 * call method to get constructs
				 */
				Method method = parent.getClass().getMethod(methodName);
				list = HashUtil.getList((Collection<T>) method.invoke(parent));
				/*
				 * sort and store list
				 */
				Collections.sort(list, comparator);
				map.put(comparator, list);
			} catch (SecurityException e) {
				throw new IndexException(e);
			} catch (NoSuchMethodException e) {
				throw new IndexException(e);
			} catch (IllegalArgumentException e) {
				throw new IndexException(e);
			} catch (IllegalAccessException e) {
				throw new IndexException(e);
			} catch (InvocationTargetException e) {
				throw new IndexException(e);
			}
		}
		return list;
	}

	/**
	 * Internal method to read dependent constructs of the given parent
	 * construct within the given range.
	 * 
	 * @param <T>
	 *            the type of dependent constructs
	 * @param key
	 *            the key of dependent constructs
	 * @param methodName
	 *            the method name which will be called if values are missing
	 * @param parent
	 *            the parent construct
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @param comparator
	 *            the comparators
	 * @return dependent constructs of the given parent construct within the
	 *         given range
	 */
	private final <T extends Construct> List<T> getConstructs(Key key, final String methodName, Construct parent, int offset, int limit,
			Comparator<T> comparator) {
		List<T> list = getConstructs(key, methodName, parent, comparator);
		return secureSubList(list, offset, limit);
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		clearCache();
		getStore().removeTopicMapListener(this);
		super.close();
	}

	/**
	 * {@inheritDoc}
	 */
	public void open() {
		super.open();
		getStore().addTopicMapListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void topicMapChanged(String id, TopicMapEventType event, Construct notifier, Object newValue, Object oldValue) {
		/*
		 * construct was removed
		 */
		if (event == TopicMapEventType.CONSTRUCT_REMOVED) {
			if (oldValue instanceof Variant) {
				clearVariantCache();
			} else if (oldValue instanceof Name) {
				clearNameCache();
			} else if (oldValue instanceof Occurrence) {
				clearOccurrenceCache();
			} else if (oldValue instanceof Topic) {
				clearTopicCache();
			} else if (oldValue instanceof Association) {
				clearAssociationCache();
			} else if (oldValue instanceof Role) {
				clearRoleCache();
			}
		}
		/*
		 * variant added
		 */
		else if (event == TopicMapEventType.VARIANT_ADDED) {
			clearVariantCache();
		}
		/*
		 * name added
		 */
		else if (event == TopicMapEventType.NAME_ADDED) {
			clearNameCache();
		}
		/*
		 * occurrence added
		 */
		else if (event == TopicMapEventType.OCCURRENCE_ADDED) {
			clearOccurrenceCache();
		}
		/*
		 * topic role added
		 */
		else if (event == TopicMapEventType.ROLE_ADDED) {
			clearRoleCache();
		}
		/*
		 * type changed
		 */
		else if (event == TopicMapEventType.TYPE_ADDED || event == TopicMapEventType.TYPE_REMOVED) {
			clearTypesCache();
		}
		/*
		 * supertype changed
		 */
		else if (event == TopicMapEventType.SUPERTYPE_ADDED || event == TopicMapEventType.SUPERTYPE_REMOVED) {
			clearSupertypesCache();
		}
		/*
		 * player modified
		 */
		else if (event == TopicMapEventType.PLAYER_MODIFIED) {
			clearAssociationCache();
		}
	}

	/**
	 * Internal method to clear the cache
	 */
	private final void clearCache() {
		if (cachedConstructs != null) {
			cachedConstructs.clear();
			cachedConstructs = null;
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.clear();
			cachedComparedConstructs = null;
		}
	}

	/**
	 * Internal method to clear the cache depends on names
	 */
	private final void clearNameCache() {
		clearVariantCache();
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.NAMES);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.NAMES);
		}
	}

	/**
	 * Internal method to clear the cache depends on occurrences
	 */
	private final void clearOccurrenceCache() {
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.OCCURRENCES);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.OCCURRENCES);
		}
	}

	/**
	 * Internal method to clear the cache depends on topic types
	 */
	private final void clearTopicCache() {
		clearTypesCache();
		clearSupertypesCache();
		clearAssociationCache();
		clearNameCache();
		clearOccurrenceCache();
	}

	/**
	 * Internal method to clear the cache depends on topic types
	 */
	private final void clearTypesCache() {
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.TYPES);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.TYPES);
		}
	}

	/**
	 * Internal method to clear the cache depends on topic supertypes
	 */
	private final void clearSupertypesCache() {
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.SUPERTYPE);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.SUPERTYPE);
		}
	}

	/**
	 * Internal method to clear the cache depends on variants
	 */
	private final void clearVariantCache() {
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.VARIANTS);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.VARIANTS);
		}
	}

	/**
	 * Internal method to clear the cache depends on roles
	 */
	private final void clearRoleCache() {
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.ROLES);
			cachedConstructs.remove(Key.ROLES_PLAYED);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.ROLES_PLAYED);
		}
	}

	/**
	 * Internal method to clear the cache depends on assocaition
	 */
	private final void clearAssociationCache() {
		clearRoleCache();
		if (cachedConstructs != null) {
			cachedConstructs.remove(Key.ASSOCIATIONS_PLAYED);
		}
		if (cachedComparedConstructs != null) {
			cachedComparedConstructs.remove(Key.ASSOCIATIONS_PLAYED);
		}
	}

	/**
	 * Clears the indexes in context to the given list, to avoid indexes out of
	 * range.
	 * 
	 * @param list
	 *            the list
	 * @param offset
	 *            the offset
	 * @param limit
	 *            the limit
	 * @return an two-
	 */
	protected final <E> List<E> secureSubList(List<E> list, int offset, int limit) {
		int from = offset;
		if (from < 0 || list.isEmpty()) {
			from = 0;
		} else if (from >= list.size()) {
			from = list.size() - 1;
		}
		int to = offset + limit;
		if (to < 0) {
			to = 0;
		} else if (to > list.size()) {
			to = list.size();
		}
		return Collections.unmodifiableList(list.subList(from, to));
	}
}
