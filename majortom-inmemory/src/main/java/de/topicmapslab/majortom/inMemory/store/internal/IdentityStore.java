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
package de.topicmapslab.majortom.inMemory.store.internal;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import de.topicmapslab.majortom.core.LocatorImpl;
import de.topicmapslab.majortom.inMemory.store.InMemoryTopicMapStore;
import de.topicmapslab.majortom.inMemory.store.model.IDataStore;
import de.topicmapslab.majortom.model.core.IConstruct;
import de.topicmapslab.majortom.model.core.ILocator;
import de.topicmapslab.majortom.model.core.ITopic;
import de.topicmapslab.majortom.model.core.ITopicMap;
import de.topicmapslab.majortom.model.event.TopicMapEventType;
import de.topicmapslab.majortom.model.exception.TopicMapStoreException;
import de.topicmapslab.majortom.model.revision.IRevision;
import de.topicmapslab.majortom.util.HashUtil;

/**
 * Base implementation of a store object containing all identity informations
 * 
 * @author Sven Krosse
 * 
 */
public class IdentityStore implements IDataStore {

	/**
	 * storage map of id-construct relation of the topic map engine
	 */
	private BidiMap ids;

	/**
	 * storage map of the reference-locator mapping of the topic map
	 */
	private Map<String, ILocator> locators;

	/**
	 * item-identifier mapping of the topic map engine
	 */
	private Map<ILocator, IConstruct> itemIdentitiers;

	/**
	 * construct to item-identifiers mapping
	 */
	private Map<IConstruct, Set<ILocator>> constructItemIdentitiers;

	/**
	 * subject-identifier mapping of the topic map engine
	 */
	private Map<ILocator, ITopic> subjectIdentifiers;

	/**
	 * topic to subject-identifiers mapping
	 */
	private Map<ITopic, Set<ILocator>> topicSubjectIdentifiers;

	/**
	 * subject-locator mapping of the topic map engine
	 */
	private Map<ILocator, ITopic> subjectLocators;

	/**
	 * topic to subject-locators mapping
	 */
	private Map<ITopic, Set<ILocator>> topicSubjectLocators;

	/**
	 * a set containing all topics
	 */
	private Set<ITopic> topics;

	/**
	 * the parent store
	 */
	private final InMemoryTopicMapStore store;

	/**
	 * constructor
	 * 
	 * @param store the parent store
	 */
	public IdentityStore(final InMemoryTopicMapStore store) {
		this.store = store;
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		if (ids != null) {
			ids.clear();
		}
		if (locators != null) {
			locators.clear();
		}
		if (itemIdentitiers != null) {
			itemIdentitiers.clear();
		}
		if (subjectIdentifiers != null) {
			subjectIdentifiers.clear();
		}
		if (subjectLocators != null) {
			subjectLocators.clear();
		}
		if (constructItemIdentitiers != null) {
			constructItemIdentitiers.clear();
		}
		if (topicSubjectIdentifiers != null) {
			topicSubjectIdentifiers.clear();
		}
		if (topicSubjectLocators != null) {
			topicSubjectLocators.clear();
		}
		if (topics != null) {
			topics.clear();
		}
	}

	/**
	 * Return the construct identified by the given id.
	 * 
	 * @param id the id
	 * @return the construct or <code>null</code>
	 */
	public IConstruct byId(final String id) {
		if (ids == null) {
			return null;
		}
		return (IConstruct) ids.get(id);
	}

	/**
	 * Return the construct identified by the given item-identifier.
	 * 
	 * @param l the item-identifier
	 * @return the construct or <code>null</code>
	 */
	public IConstruct byItemIdentifier(final ILocator l) {
		if (itemIdentitiers == null) {
			return null;
		}
		return itemIdentitiers.get(l);
	}

	/**
	 * Return the topic identified by the given subject-identifier.
	 * 
	 * @param l the subject-identifier
	 * @return the topic or <code>null</code>
	 */
	public ITopic bySubjectIdentifier(final ILocator l) {
		if (subjectIdentifiers == null) {
			return null;
		}
		return subjectIdentifiers.get(l);
	}

	/**
	 * Return the topic identified by the given subject-locator.
	 * 
	 * @param l the subject-locator
	 * @return the topic or <code>null</code>
	 */
	public ITopic bySubjectLocator(final ILocator l) {
		if (subjectLocators == null) {
			return null;
		}
		return subjectLocators.get(l);
	}

	/**
	 * Return all item-identifiers of the given construct.
	 * 
	 * @param c the construct
	 * @return the identifiers
	 */
	public Set<ILocator> getItemIdentifiers(IConstruct c) {
		if (constructItemIdentitiers == null || !constructItemIdentitiers.containsKey(c)) {
			return HashUtil.getHashSet();
		}
		return constructItemIdentitiers.get(c);
	}

	/**
	 * Return all subject-identifiers of the given topic.
	 * 
	 * @param t the topic
	 * @return the identifiers
	 */
	public Set<ILocator> getSubjectIdentifiers(ITopic t) {
		if (topicSubjectIdentifiers == null || !topicSubjectIdentifiers.containsKey(t)) {
			return HashUtil.getHashSet();
		}
		return topicSubjectIdentifiers.get(t);
	}

	/**
	 * Return all subject-locator of the given topic.
	 * 
	 * @param t the topic
	 * @return the locators
	 */
	public Set<ILocator> getSubjectLocators(ITopic t) {
		if (topicSubjectLocators == null || !topicSubjectLocators.containsKey(t)) {
			return HashUtil.getHashSet();
		}
		return topicSubjectLocators.get(t);
	}

	/**
	 * Register the id for the given construct
	 * 
	 * @param c the construct
	 * @param id the id
	 */
	public void setId(final IConstruct c, final String id) {
		if (ids == null) {
			ids = new TreeBidiMap();
		}
		if (c instanceof ITopic) {
			if (topics == null) {
				topics = HashUtil.getHashSet();
			}
			this.topics.add((ITopic) c);
		}
		this.ids.put(id, c);
	}

	/**
	 * Register a item-identifier for the given construct
	 * 
	 * @param c the construct
	 * @param identifier the item-identifier
	 */
	public void addItemIdentifer(final IConstruct c, final ILocator identifier) {
		if (itemIdentitiers == null) {
			itemIdentitiers = HashUtil.getHashMap();
		}
		this.itemIdentitiers.put(identifier, c);

		/*
		 * store backward relation
		 */
		if (constructItemIdentitiers == null) {
			constructItemIdentitiers = HashUtil.getHashMap();
		}
		Set<ILocator> set = constructItemIdentitiers.get(c);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(identifier);
		constructItemIdentitiers.put(c, set);
	}

	/**
	 * Register a subject-identifier for the given topic
	 * 
	 * @param t the topic
	 * @param identifier the subject-identifier
	 */
	public void addSubjectIdentifier(final ITopic t, final ILocator identifier) {
		if (subjectIdentifiers == null) {
			subjectIdentifiers = HashUtil.getHashMap();
		}
		this.subjectIdentifiers.put(identifier, t);

		/*
		 * store backward relation
		 */
		if (topicSubjectIdentifiers == null) {
			topicSubjectIdentifiers = HashUtil.getHashMap();
		}
		Set<ILocator> set = topicSubjectIdentifiers.get(t);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(identifier);
		topicSubjectIdentifiers.put(t, set);
	}

	/**
	 * Register a subject-locator for the given topic
	 * 
	 * @param t the topic
	 * @param locator the subject-locator
	 */
	public void addSubjectLocator(final ITopic t, final ILocator locator) {
		if (subjectLocators == null) {
			subjectLocators = HashUtil.getHashMap();
		}
		this.subjectLocators.put(locator, t);

		/*
		 * store backward relation
		 */
		if (topicSubjectLocators == null) {
			topicSubjectLocators = HashUtil.getHashMap();
		}
		Set<ILocator> set = topicSubjectLocators.get(t);
		if (set == null) {
			set = HashUtil.getHashSet();
		}
		set.add(locator);
		topicSubjectLocators.put(t, set);
	}

	/**
	 * Unregister the id of the construct.
	 * 
	 * @param c the construct
	 * @param id the id
	 */
	public void removeId(final IConstruct c, final String id) {
		if (ids == null) {
			throw new TopicMapStoreException("Id is unknown and cannot remove.");
		}
		this.ids.remove(id);
	}

	/**
	 * Unregister a item-identifier for the given construct
	 * 
	 * @param c the construct
	 * @param identifier the item-identifier
	 */
	public void removeItemIdentifer(final IConstruct c, final ILocator identifier) {
		if (itemIdentitiers == null) {
			throw new TopicMapStoreException("Identifier is unknown and cannot remove.");
		}
		this.itemIdentitiers.remove(identifier);

		/*
		 * remove backward relation
		 */
		Set<ILocator> set = constructItemIdentitiers.get(c);
		set.remove(identifier);
		constructItemIdentitiers.put(c, set);
	}

	/**
	 * Unregister a subject-identifier for the given topic
	 * 
	 * @param t the topic
	 * @param identifier the subject-identifier
	 */
	public void removeSubjectIdentifier(final ITopic t, final ILocator identifier) {
		if (subjectIdentifiers == null) {
			throw new TopicMapStoreException("Identifier is unknown and cannot remove.");
		}
		this.subjectIdentifiers.remove(identifier);

		/*
		 * remove backward relation
		 */
		Set<ILocator> set = topicSubjectIdentifiers.get(t);
		set.remove(identifier);
		topicSubjectIdentifiers.put(t, set);
	}

	/**
	 * Unregister a subject-locator for the given topic
	 * 
	 * @param t the topic
	 * @param locator the subject-locator
	 */
	public void removeSubjectLocator(final ITopic t, final ILocator identifier) {
		if (subjectLocators == null) {
			throw new TopicMapStoreException("Identifier is unknown and cannot remove.");
		}
		this.subjectLocators.remove(identifier);

		/*
		 * remove backward relation
		 */
		Set<ILocator> set = topicSubjectLocators.get(t);
		set.remove(identifier);
		topicSubjectLocators.put(t, set);
	}

	/**
	 * Remove the construct from the internal store
	 * 
	 * @param c the construct
	 */
	public void removeConstruct(IConstruct c) {
		if (c instanceof ITopic) {
			removeTopic((ITopic) c);
		} else {
			/*
			 * Remove item identifiers
			 */
			if (constructItemIdentitiers != null && constructItemIdentitiers.containsKey(c)) {
				for (ILocator l : constructItemIdentitiers.get(c)) {
					itemIdentitiers.remove(l);
				}
				constructItemIdentitiers.remove(c);
			}
			/*
			 * remove id
			 */
			ids.removeValue(c);
		}
	}

	/**
	 * Removing the given topic from internal store
	 * 
	 * @param t the topic
	 */
	public void removeTopic(ITopic t) {
		/*
		 * Remove item identifiers
		 */
		if (constructItemIdentitiers != null && constructItemIdentitiers.containsKey(t)) {
			for (ILocator l : constructItemIdentitiers.get(t)) {
				itemIdentitiers.remove(l);
			}
			constructItemIdentitiers.remove(t);
		}
		/*
		 * remove subject-identifiers
		 */
		if (topicSubjectIdentifiers != null && topicSubjectIdentifiers.containsKey(t)) {
			for (ILocator l : topicSubjectIdentifiers.get(t)) {
				subjectIdentifiers.remove(l);
			}
			topicSubjectIdentifiers.remove(t);
		}
		/*
		 * remove subject-locators
		 */
		if (topicSubjectLocators != null && topicSubjectLocators.containsKey(t)) {
			for (ILocator l : topicSubjectLocators.get(t)) {
				subjectLocators.remove(l);
			}
			topicSubjectLocators.remove(t);
		}

		/*
		 * remove topic
		 */
		if (topics != null) {
			topics.remove(t);
		}

		/*
		 * remove id
		 */
		ids.removeValue(t);
	}

	/**
	 * Creates a new locator instance of the reference is unknown or return the
	 * stored instance
	 * 
	 * @param reference the reference
	 * @return the locator
	 */
	public ILocator createLocator(final String reference) {
		if (locators == null) {
			locators = HashUtil.getHashMap();
		} else if (locators.containsKey(reference)) {
			return locators.get(reference);
		}
		ILocator l = new LocatorImpl(reference);
		locators.put(reference, l);
		return l;
	}

	/**
	 * Returns all topics of the internal store
	 * 
	 * @return all topics
	 */
	public Set<ITopic> getTopics() {
		if (topics == null) {
			return HashUtil.getHashSet();
		}
		return topics;
	}

	/**
	 * Create a random item-identifier.
	 * 
	 * @param topicMap the topic map
	 * @return the item-identifier
	 */
	public ILocator createItemIdentifier(ITopicMap topicMap) {
		String itemIdentifier = topicMap.getLocator().getReference() + "/" + UUID.randomUUID().toString();
		return createLocator(itemIdentifier);
	}

	public <T extends IConstruct> void replace(T construct, T replacement) {
		if (construct instanceof ITopic) {
			replace((ITopic) construct, (ITopic) replacement);
		} else if (constructItemIdentitiers != null && constructItemIdentitiers.containsKey(construct)) {
			Set<ILocator> itemIdentifiers = HashUtil.getHashSet(getItemIdentifiers(construct));
			/*
			 * move all item-identifier
			 */
			for (ILocator itemIdentifier : itemIdentifiers) {
				this.itemIdentitiers.put(itemIdentifier, replacement);
			}
			if (this.constructItemIdentitiers.containsKey(replacement)) {
				itemIdentifiers.addAll(this.constructItemIdentitiers.get(replacement));
			}
			constructItemIdentitiers.remove(construct);
			constructItemIdentitiers.put(replacement, itemIdentifiers);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void replace(ITopic topic, ITopic replacement, IRevision revision) {
		Set<ILocator> itemIdentifiers = HashUtil.getHashSet(getItemIdentifiers(topic));
		Set<ILocator> subjectIdentifiers = HashUtil.getHashSet(getSubjectIdentifiers(topic));
		Set<ILocator> subjectLocators = HashUtil.getHashSet(getSubjectLocators(topic));

		/*
		 * move all item-identifier
		 */
		for (ILocator itemIdentifier : itemIdentifiers) {
			removeItemIdentifer(topic, itemIdentifier);
			addItemIdentifer(replacement, itemIdentifier);
			/*
			 * store revision
			 */
			store.storeRevision(revision, TopicMapEventType.ITEM_IDENTIFIER_REMOVED, topic, null, itemIdentifier);
			store.storeRevision(revision, TopicMapEventType.ITEM_IDENTIFIER_ADDED, replacement, itemIdentifier, null);
		}

		/*
		 * move all subject-identifier
		 */
		for (ILocator subjectIdentifier : subjectIdentifiers) {
			removeSubjectIdentifier(topic, subjectIdentifier);
			addSubjectIdentifier(replacement, subjectIdentifier);
			/*
			 * store revision
			 */
			store.storeRevision(revision, TopicMapEventType.SUBJECT_IDENTIFIER_REMOVED, topic, null, subjectIdentifier);
			store.storeRevision(revision, TopicMapEventType.SUBJECT_IDENTIFIER_ADDED, replacement, subjectIdentifier, null);
		}
		/*
		 * move all subject-locator
		 */
		for (ILocator subjectLocator : subjectLocators) {
			removeSubjectLocator(topic, subjectLocator);
			addSubjectLocator(replacement, subjectLocator);
			/*
			 * store revision
			 */
			store.storeRevision(revision, TopicMapEventType.SUBJECT_LOCATOR_REMOVED, topic, null, subjectLocator);
			store.storeRevision(revision, TopicMapEventType.SUBJECT_LOCATOR_ADDED, replacement, subjectLocator, null);
		}
	}

	/**
	 * Return all internal stored identifiers.
	 * 
	 * @return all identifiers
	 */
	public Set<ILocator> getIdentifiers() {
		Set<ILocator> set = HashUtil.getHashSet();
		set.addAll(getItemIdentifiers());
		set.addAll(getSubjectIdentifiers());
		set.addAll(getSubjectLocators());
		return set;
	}

	/**
	 * Return all internal stored item-identifiers.
	 * 
	 * @return all item-identifiers
	 */
	public Set<ILocator> getItemIdentifiers() {
		if (itemIdentitiers == null) {
			return HashUtil.getHashSet();
		}
		return itemIdentitiers.keySet();
	}

	/**
	 * Return all internal stored subject-identifiers.
	 * 
	 * @return all subject-identifiers
	 */
	public Set<ILocator> getSubjectIdentifiers() {
		if (subjectIdentifiers == null) {
			return HashUtil.getHashSet();
		}
		return subjectIdentifiers.keySet();
	}

	/**
	 * Return all internal stored subject-locators.
	 * 
	 * @return all subject-locators
	 */
	public Set<ILocator> getSubjectLocators() {
		if (subjectLocators == null) {
			return HashUtil.getHashSet();
		}
		return subjectLocators.keySet();
	}

	/**
	 * Checks if the given locator is used as identifier.
	 * 
	 * @param locator the identifier
	 * @return <code>true</code> if the locator is used as identifier,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsIdentifier(ILocator locator) {
		return containsItemIdentifier(locator) || containsSubjectIdentifier(locator) || containsSubjectLocator(locator);
	}

	/**
	 * Checks if the given locator is used as item-identifier.
	 * 
	 * @param locator the item-identifier
	 * @return <code>true</code> if the locator is used as item-identifier,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsItemIdentifier(ILocator locator) {
		if (itemIdentitiers == null) {
			return false;
		}
		return itemIdentitiers.containsKey(locator);
	}

	/**
	 * Checks if the given locator is used as subject-identifier.
	 * 
	 * @param locator the subject-identifier
	 * @return <code>true</code> if the locator is used as subject-identifier,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsSubjectIdentifier(ILocator locator) {
		if (subjectIdentifiers == null) {
			return false;
		}
		return subjectIdentifiers.containsKey(locator);
	}

	/**
	 * Checks if the given locator is used as subject-locator.
	 * 
	 * @param locator the subject-locator
	 * @return <code>true</code> if the locator is used as subject-locator,
	 *         <code>false</code> otherwise.
	 */
	public boolean containsSubjectLocator(ILocator locator) {
		if (subjectLocators == null) {
			return false;
		}
		return subjectLocators.containsKey(locator);
	}

	/**
	 * Return the internal stored id of the given construct
	 * 
	 * @param construct the construct
	 * @return the internal id and never <code>null</code>. If the construct is
	 *         unknown an exception will be thrown.
	 */
	public String getId(IConstruct construct) {
		if (ids == null || !ids.containsValue(construct)) {
			throw new TopicMapStoreException("Unkown construct instance.");
		}
		return (String) ids.getKey(construct);
	}

	/**
	 * Return the internal stored store instance.
	 * 
	 * @return the store the store instance
	 */
	protected InMemoryTopicMapStore getStore() {
		return store;
	}

	/**
	 * Checks if the store know at least one subject locator for the given topic
	 * 
	 * @param topic the topic
	 * @return <code>true</code> if at least one subject locator is store for
	 *         the given topic, <code>false</code> otherwise.
	 */
	protected boolean containsSubjectLocators(ITopic topic) {
		if (topicSubjectLocators == null) {
			return false;
		}
		return topicSubjectLocators.containsKey(topic);
	}

	/**
	 * Checks if the store know at least one subject identifier for the given
	 * topic
	 * 
	 * @param topic the topic
	 * @return <code>true</code> if at least one subject identifier is store for
	 *         the given topic, <code>false</code> otherwise.
	 */
	protected boolean containsSubjectIdentifiers(ITopic topic) {
		if (topicSubjectIdentifiers == null) {
			return false;
		}
		return topicSubjectIdentifiers.containsKey(topic);
	}

	/**
	 * Checks if the store know at least one item identifier for the given
	 * construct
	 * 
	 * @param construct the construct
	 * @return <code>true</code> if at least one item identifier is store for
	 *         the given construct, <code>false</code> otherwise.
	 */
	protected boolean containsItemIdentifiers(IConstruct construct) {
		if (constructItemIdentitiers == null) {
			return false;
		}
		return constructItemIdentitiers.containsKey(construct);
	}
}
