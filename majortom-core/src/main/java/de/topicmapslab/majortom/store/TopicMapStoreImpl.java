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
package de.topicmapslab.majortom.store;

import java.util.Calendar;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.tmapi.core.Construct;
import org.tmapi.core.FeatureNotRecognizedException;
import org.tmapi.core.Topic;
import org.tmapi.core.TopicMap;

import de.topicmapslab.majortom.executable.EventNotifier;
import de.topicmapslab.majortom.model.core.IAssociation;
import de.topicmapslab.majortom.model.core.IAssociationRole;
import de.topicmapslab.majortom.model.core.ICharacteristics;
import de.topicmapslab.majortom.model.core.IConstruct;
import de.topicmapslab.majortom.model.core.IDatatypeAware;
import de.topicmapslab.majortom.model.core.ILocator;
import de.topicmapslab.majortom.model.core.IName;
import de.topicmapslab.majortom.model.core.IOccurrence;
import de.topicmapslab.majortom.model.core.IReifiable;
import de.topicmapslab.majortom.model.core.IScopable;
import de.topicmapslab.majortom.model.core.IScope;
import de.topicmapslab.majortom.model.core.ITopic;
import de.topicmapslab.majortom.model.core.ITopicMap;
import de.topicmapslab.majortom.model.core.ITopicMapSystem;
import de.topicmapslab.majortom.model.core.ITypeable;
import de.topicmapslab.majortom.model.core.IVariant;
import de.topicmapslab.majortom.model.event.ITopicMapListener;
import de.topicmapslab.majortom.model.event.TopicMapEventType;
import de.topicmapslab.majortom.model.exception.OperationSignatureException;
import de.topicmapslab.majortom.model.exception.TopicMapStoreException;
import de.topicmapslab.majortom.model.exception.UnmodifyableStoreException;
import de.topicmapslab.majortom.model.revision.Changeset;
import de.topicmapslab.majortom.model.revision.IRevision;
import de.topicmapslab.majortom.model.store.ITopicMapStore;
import de.topicmapslab.majortom.model.store.TopicMapStoreParameterType;
import de.topicmapslab.majortom.util.FeatureStrings;
import de.topicmapslab.majortom.util.HashUtil;

/**
 * Base implementation of {@link ITopicMapStore}.
 * 
 * @author Sven Krosse
 * 
 */
public abstract class TopicMapStoreImpl implements ITopicMapStore {

	private boolean connected = false;

	private Set<ITopicMapListener> listeners = null;
	private ITopicMapSystem topicMapSystem;
	private ITopicMap topicMap;
	private ThreadPoolExecutor threadPool;

	/**
	 * constructor
	 */
	public TopicMapStoreImpl() {
	}

	/**
	 * constructor
	 * 
	 * @param topicMapSystem
	 *            the parent system
	 */
	public TopicMapStoreImpl(final ITopicMapSystem topicMapSystem) {
		setTopicMapSystem(topicMapSystem);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addTopicMapListener(ITopicMapListener listener) {
		if (listeners == null) {
			listeners = HashUtil.getHashSet();
		}
		listeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeTopicMapListener(ITopicMapListener listener) {
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Object doCreate(IConstruct context, TopicMapStoreParameterType paramType, Object... params) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		if (isReadOnly() && paramType != TopicMapStoreParameterType.LOCATOR && paramType != TopicMapStoreParameterType.SCOPE) {
			throw new UnmodifyableStoreException("Creation not supported by read-only stores!");
		}
		switch (paramType) {
		case ASSOCIATION: {
			if (context instanceof ITopicMap) {
				/*
				 * createAssociation(ITopic)
				 */
				if (params.length == 1 && params[0] instanceof ITopic) {
					return doCreateAssociation((ITopicMap) context, (ITopic) params[0]);
				}
				/*
				 * createAssociation(ITopic, ITopic[])
				 */
				else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof Topic[]) {
					return doCreateAssociation((ITopicMap) context, (ITopic) params[0], convert((Topic[]) params[1]));
				}
				/*
				 * createAssociation(ITopic, Collection<ITopic>)
				 */
				else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof Collection<?>) {
					return doCreateAssociation((ITopicMap) context, (ITopic) params[0], (Collection<ITopic>) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_ITEM_IDENTIFER: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doCreateTopicByItemIdentifier((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_SUBJECT_IDENTIFER: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doCreateTopicBySubjectIdentifier((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_SUBJECT_LOCATOR: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doCreateTopicBySubjectLocator((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case LOCATOR: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof String) {
				return doCreateLocator((ITopicMap) context, (String) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case NAME: {
			if (context instanceof ITopic) {
				/*
				 * createName(String)
				 */
				if (params.length == 1 && params[0] instanceof String) {
					return doCreateName((ITopic) context, (String) params[0]);
				}
				/*
				 * createName(String, ITopic[])
				 */
				else if (params.length == 2 && params[0] instanceof String && params[1] instanceof Topic[]) {
					return doCreateName((ITopic) context, (String) params[0], convert((Topic[]) params[1]));
				}
				/*
				 * createName(String, Collection<ITopic>)
				 */
				else if (params.length == 2 && params[0] instanceof String && params[1] instanceof Collection<?>) {
					return doCreateName((ITopic) context, (String) params[0], (Collection<ITopic>) params[1]);
				}
				/*
				 * createName(ITopic, String)
				 */
				else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof String) {
					return doCreateName((ITopic) context, (ITopic) params[0], (String) params[1]);
				}

				/*
				 * createName(ITopic,String, ITopic[])
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof Topic[]) {
					return doCreateName((ITopic) context, (ITopic) params[0], (String) params[1], convert((Topic[]) params[2]));
				}
				/*
				 * createName(ITopic,String, Collection<ITopic>)
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof Collection<?>) {
					return doCreateName((ITopic) context, (ITopic) params[0], (String) params[1], (Collection<ITopic>) params[2]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case OCCURRENCE: {
			if (context instanceof ITopic) {
				/*
				 * createOccurrence(ITopic,String)
				 */
				if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof String) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1]);
				}
				/*
				 * createOccurrence(ITopic,String)
				 */
				else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof ILocator) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (ILocator) params[1]);
				}
				/*
				 * createOccurrence(ITopic,String, ILocator)
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof ILocator) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1], (ILocator) params[2]);
				}
				/*
				 * createOccurrence(ITopic,String, ITopic[])
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof Topic[]) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1], convert((Topic[]) params[2]));
				}
				/*
				 * createOccurrence(ITopic,String, Collection<?>)
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof Collection<?>) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1], (Collection<ITopic>) params[2]);
				}
				/*
				 * createOccurrence(ITopic,Locator, ITopic[])
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof ILocator && params[2] instanceof Topic[]) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (ILocator) params[1], convert((Topic[]) params[2]));
				}
				/*
				 * createOccurrence(ITopic,Locator, Collection<?>)
				 */
				else if (params.length == 3 && params[0] instanceof ITopic && params[1] instanceof ILocator && params[2] instanceof Collection<?>) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (ILocator) params[1], (Collection<ITopic>) params[2]);
				}
				/*
				 * createOccurrence(ITopic, String, ILocator, ITopic[])
				 */
				else if (params.length == 4 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof ILocator
						&& params[3] instanceof Topic[]) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1], (ILocator) params[2], convert((Topic[]) params[3]));
				}
				/*
				 * createOccurrence(ITopic,String, ILocator, Collection<?>)
				 */
				else if (params.length == 4 && params[0] instanceof ITopic && params[1] instanceof String && params[2] instanceof ILocator
						&& params[3] instanceof Collection<?>) {
					return doCreateOccurrence((ITopic) context, (ITopic) params[0], (String) params[1], (ILocator) params[2], (Collection<ITopic>) params[3]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case VARIANT: {
			if (context instanceof IName) {
				/*
				 * createVariant(String,ITopic[])
				 */
				if (params.length == 2 && params[0] instanceof String && params[1] instanceof Topic[]) {
					return doCreateVariant((IName) context, (String) params[0], convert((Topic[]) params[1]));
				}
				/*
				 * createVariant(String,Collection<?>)
				 */
				else if (params.length == 2 && params[0] instanceof String && params[1] instanceof Collection<?>) {
					return doCreateVariant((IName) context, (String) params[0], (Collection<ITopic>) params[1]);
				}
				/*
				 * createVariant(ILocator,ITopic[])
				 */
				else if (params.length == 2 && params[0] instanceof ILocator && params[1] instanceof Topic[]) {
					return doCreateVariant((IName) context, (ILocator) params[0], convert((Topic[]) params[1]));
				}
				/*
				 * createVariant(ILocator,Collection<?>)
				 */
				else if (params.length == 2 && params[0] instanceof ILocator && params[1] instanceof Collection<?>) {
					return doCreateVariant((IName) context, (ILocator) params[0], (Collection<ITopic>) params[1]);
				}
				/*
				 * createVariant(String, ILocator,ITopic[])
				 */
				else if (params.length == 3 && params[0] instanceof String && params[1] instanceof ILocator && params[2] instanceof Topic[]) {
					return doCreateVariant((IName) context, (String) params[0], (ILocator) params[1], convert((Topic[]) params[2]));
				}
				/*
				 * createVariant(String, ILocator,Collection<?>)
				 */
				else if (params.length == 3 && params[0] instanceof String && params[1] instanceof ILocator && params[2] instanceof Collection<?>) {
					return doCreateVariant((IName) context, (String) params[0], (ILocator) params[1], (Collection<ITopic>) params[2]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case ROLE: {
			if (context instanceof IAssociation && params.length == 2 && params[0] instanceof ITopic && params[1] instanceof ITopic) {
				return doCreateRole((IAssociation) context, (ITopic) params[0], (ITopic) params[1]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case TOPIC: {
			if (context instanceof ITopicMap) {
				return doCreateTopicByItemIdentifier((ITopicMap) context, doCreateItemIdentifier((ITopicMap) context));
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case SCOPE: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof Collection<?>) {
				return doCreateScope((ITopicMap) context, (Collection<ITopic>) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		}
		throw new OperationSignatureException(context, paramType, params);
	}

	/**
	 * Creates an internal item-identifier for a construct.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @return the created item-identifier
	 */
	protected abstract ILocator doCreateItemIdentifier(ITopicMap topicMap);

	/**
	 * Create a new association item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param type
	 *            the type
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IAssociation doCreateAssociation(ITopicMap topicMap, ITopic type) throws TopicMapStoreException;

	/**
	 * Create a new association item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param type
	 *            the type
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IAssociation doCreateAssociation(ITopicMap topicMap, ITopic type, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new name characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param value
	 *            the characteristics value
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IName doCreateName(ITopic topic, String value) throws TopicMapStoreException;

	/**
	 * Create a new name characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param value
	 *            the characteristics value
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IName doCreateName(ITopic topic, String value, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new name characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IName doCreateName(ITopic topic, ITopic type, String value) throws TopicMapStoreException;

	/**
	 * Create a new name characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IName doCreateName(ITopic topic, ITopic type, String value, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, String value) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, String value, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, ILocator value) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, ILocator value, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @param datatype
	 *            the data type
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, String value, ILocator datatype) throws TopicMapStoreException;

	/**
	 * Create a new occurrence characteristics item.
	 * 
	 * @param topic
	 *            the parent topic
	 * @param type
	 *            the type
	 * @param value
	 *            the characteristics value
	 * @param datatype
	 *            the data type
	 * @param themes
	 *            the scoping themes
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IOccurrence doCreateOccurrence(ITopic topic, ITopic type, String value, ILocator datatype, Collection<ITopic> themes)
			throws TopicMapStoreException;

	/**
	 * Create a new association role item.
	 * 
	 * @param association
	 *            the parent association
	 * @param type
	 *            the type
	 * @param player
	 *            the role player
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IAssociationRole doCreateRole(IAssociation association, ITopic type, ITopic player) throws TopicMapStoreException;

	/**
	 * Create the internal scope object representing the collection of themes
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param themes
	 *            the themes collection
	 * @return the scope object
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IScope doCreateScope(ITopicMap topicMap, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new topic item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doCreateTopic(ITopicMap topicMap) throws TopicMapStoreException;

	/**
	 * Create a new topic item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param itemIdentifier
	 *            the item-identifier
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doCreateTopicByItemIdentifier(ITopicMap topicMap, ILocator itemIdentifier) throws TopicMapStoreException;

	/**
	 * Create a new topic item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param subjectIdentifier
	 *            the subject-identifier
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doCreateTopicBySubjectIdentifier(ITopicMap topicMap, ILocator subjectIdentifier) throws TopicMapStoreException;

	/**
	 * Create a new topic item.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param subjectLocator
	 *            the subject-locator
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doCreateTopicBySubjectLocator(ITopicMap topicMap, ILocator subjectLocator) throws TopicMapStoreException;

	/**
	 * Create a locator instance.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param reference
	 *            the string reference
	 * @return the created construct
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ILocator doCreateLocator(ITopicMap topicMap, String reference) throws TopicMapStoreException;

	/**
	 * Create a new variant for the given name.
	 * 
	 * @param name
	 *            the parent name
	 * @param value
	 *            the value
	 * @param themes
	 *            the scoping themes
	 * @return the created variant
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IVariant doCreateVariant(IName name, String value, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new variant for the given name.
	 * 
	 * @param name
	 *            the parent name
	 * @param datatype
	 *            the data type
	 * @param themes
	 *            the scoping themes
	 * @return the created variant
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IVariant doCreateVariant(IName name, ILocator datatype, Collection<ITopic> themes) throws TopicMapStoreException;

	/**
	 * Create a new variant for the given name.
	 * 
	 * @param name
	 *            the parent name
	 * @param value
	 *            the value
	 * @param datatype
	 *            the data type
	 * @param themes
	 *            the scoping themes
	 * @return the created variant
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IVariant doCreateVariant(IName name, String value, ILocator datatype, Collection<ITopic> themes) throws TopicMapStoreException;

	// *********************
	// * MODIFY OPERATIONS *
	// *********************

	/**
	 * {@inheritDoc}
	 */
	public void doModify(IConstruct context, TopicMapStoreParameterType paramType, Object... params) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		if (isReadOnly()) {
			throw new UnmodifyableStoreException("Modfication operation not supported by read-only stores!");
		}
		switch (paramType) {
		case ITEM_IDENTIFIER: {
			if (params.length == 1 && params[0] instanceof ILocator) {
				doModifyItemIdentifier(context, (ILocator) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case PLAYER: {
			if (context instanceof IAssociationRole && params.length == 1 && params[0] instanceof ITopic) {
				doModifyPlayer((IAssociationRole) context, (ITopic) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case REIFICATION: {
			if (context instanceof IReifiable && params.length == 1 && params[0] instanceof ITopic) {
				doModifyReifier((IReifiable) context, (ITopic) params[0]);
			} else if (context instanceof IReifiable && params.length == 1 && params[0] == null) {
				doModifyReifier((IReifiable) context, null);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case SCOPE: {
			if (context instanceof IScopable && params.length == 1 && params[0] instanceof ITopic) {
				doModifyScope((IScopable) context, (ITopic) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case SUBJECT_IDENTIFIER: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ILocator) {
				doModifySubjectIdentifier((ITopic) context, (ILocator) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case SUBJECT_LOCATOR: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ILocator) {
				doModifySubjectLocator((ITopic) context, (ILocator) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case SUPERTYPE: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ITopic) {
				doModifySupertype((ITopic) context, (ITopic) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case TAG: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof String) {
				doModifyTag((ITopicMap) context, (String) params[0]);
			} else if (context instanceof ITopicMap && params.length == 2 && params[0] instanceof String && params[1] instanceof Calendar) {
				doModifyTag((ITopicMap) context, (String) params[0], (Calendar) params[1]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case TYPE: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ITopic) {
				doModifyType((ITopic) context, (ITopic) params[0]);
			} else if (context instanceof ITypeable && params.length == 1 && params[0] instanceof ITopic) {
				doModifyType((ITypeable) context, (ITopic) params[0]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		case VALUE: {
			if (context instanceof IName && params.length == 1 && params[0] instanceof String) {
				doModifyValue((IName) context, (String) params[0]);
			} else if (context instanceof IDatatypeAware && params.length == 1 && params[0] instanceof String) {
				doModifyValue((IDatatypeAware) context, (String) params[0]);
			} else if (context instanceof IDatatypeAware && params.length == 1) {
				doModifyValue((IDatatypeAware) context, params[0]);
			} else if (context instanceof IDatatypeAware && params.length == 2 && params[0] instanceof String && params[1] instanceof ILocator) {
				doModifyValue((IDatatypeAware) context, (String) params[0], (ILocator) params[1]);
			} else {
				throw new OperationSignatureException(context, paramType, params);
			}
		}
			break;
		default: {
			throw new OperationSignatureException(context, paramType, params);
		}
		}

	}

	/**
	 * Add a new item-identifier to the given construct
	 * 
	 * @param c
	 *            the construct
	 * @param itemIdentifier
	 *            the item identifier
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyItemIdentifier(IConstruct c, ILocator itemIdentifier) throws TopicMapStoreException;

	/**
	 * Modify the player of the given role.
	 * 
	 * @param role
	 *            the role
	 * @param player
	 *            the new player
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyPlayer(IAssociationRole role, ITopic player) throws TopicMapStoreException;

	/**
	 * Modify the reifier of the given reified item.
	 * 
	 * @param r
	 *            the reified item
	 * @param reifier
	 *            the reifier
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyReifier(IReifiable r, ITopic reifier) throws TopicMapStoreException;

	/**
	 * Add a new theme to the given scoped item.
	 * 
	 * @param s
	 *            the scoped item
	 * @param theme
	 *            the theme
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyScope(IScopable s, ITopic theme) throws TopicMapStoreException;

	/**
	 * Add a new subject-identifier to the given topic
	 * 
	 * @param t
	 *            the topic
	 * @param subjectIdentifier
	 *            the subject-identifier
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifySubjectIdentifier(ITopic t, ILocator subjectIdentifier) throws TopicMapStoreException;

	/**
	 * Add a new subject-locator to the given topic
	 * 
	 * @param t
	 *            the topic
	 * @param subjectLocator
	 *            the subject-locator
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifySubjectLocator(ITopic t, ILocator subjectLocator) throws TopicMapStoreException;

	/**
	 * Add a new super type to the given topic
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the super type
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifySupertype(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Add a new tag to the given topic map for the current time stamp
	 * 
	 * @param tm
	 *            the topic map
	 * @param tag
	 *            the tag
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyTag(ITopicMap tm, String tag) throws TopicMapStoreException;

	/**
	 * Add a new tag to the given topic map for the given time stamp
	 * 
	 * @param tm
	 *            the topic map
	 * @param tag
	 *            the tag
	 * @param timestamp
	 *            the time stamp
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyTag(ITopicMap tm, String tag, Calendar timestamp) throws TopicMapStoreException;

	/**
	 * Modify the type of the given typed item.
	 * 
	 * @param t
	 *            the typed item
	 * @param type
	 *            the type
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyType(ITypeable t, ITopic type) throws TopicMapStoreException;

	/**
	 * Add a new type to the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type to add
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyType(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Modify the value of the given name.
	 * 
	 * @param n
	 *            the name
	 * @param value
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyValue(IName n, String value) throws TopicMapStoreException;

	/**
	 * Modify the value of the given data-type-aware.
	 * 
	 * @param t
	 *            data-type-aware
	 * @param value
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyValue(IDatatypeAware t, String value) throws TopicMapStoreException;

	/**
	 * Modify the value and the data type of the given data-type-aware.
	 * 
	 * @param t
	 *            data-type-aware
	 * @param value
	 *            the value
	 * @param datatype
	 *            the data type
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyValue(IDatatypeAware t, String value, ILocator datatype) throws TopicMapStoreException;

	/**
	 * Modify the value and the data type of the given data-type-aware.
	 * 
	 * @param t
	 *            data-type-aware
	 * @param value
	 *            the value
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doModifyValue(IDatatypeAware t, Object value) throws TopicMapStoreException;

	// ********************
	// * MERGE OPERATIONS *
	// ********************

	/**
	 * {@inheritDoc}
	 */
	public <T extends Construct> void doMerge(T context, T... others) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		if (isReadOnly()) {
			throw new UnmodifyableStoreException("Merge operation not supported by read-only stores!");
		}
		if (context instanceof TopicMap && others.length > 0) {
			for (T other : others) {
				if (other instanceof TopicMap) {
					doMergeTopicMaps((TopicMap) context, (TopicMap) other);
				} else {
					throw new TopicMapStoreException("Cannot merge other constructs than topic maps into a topic map.");
				}
			}
		} else if (context instanceof ITopic && others.length > 0) {
			for (T other : others) {
				if (other instanceof ITopic) {
					doMergeTopics((ITopic) context, (ITopic) other);
				} else {
					throw new TopicMapStoreException("Cannot merge other constructs than topics into a topic.");
				}
			}
		} else {
			throw new TopicMapStoreException("Cannot merge other constructs than topics or topic maps into a topic.");
		}
	}

	/**
	 * Merge a topic in the given topic
	 * 
	 * @param context
	 *            the topic to merge in
	 * @param other
	 *            the topic to merge in
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doMergeTopics(ITopic context, ITopic other) throws TopicMapStoreException;

	/**
	 * Merge a topic map in the given topic map.
	 * 
	 * @param context
	 *            the topic map
	 * @param other
	 *            the topic map to merge in
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doMergeTopicMaps(TopicMap context, TopicMap other) throws TopicMapStoreException;

	// *******************
	// * READ OPERATIONS *
	// *******************

	/**
	 * {@inheritDoc}
	 */
	public Object doRead(IConstruct context, TopicMapStoreParameterType paramType, Object... params) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		switch (paramType) {
		case ROLE_TYPES: {
			if (context instanceof IAssociation) {
				return doReadRoleTypes((IAssociation) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case ROLE: {
			if (context instanceof IAssociation) {
				if (params.length == 0) {
					return doReadRoles((IAssociation) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadRoles((IAssociation) context, (ITopic) params[0]);
				}
			} else if (context instanceof ITopic) {
				if (params.length == 0) {
					return doReadRoles((ITopic) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadRoles((ITopic) context, (ITopic) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof ITopic) {
					return doReadRoles((ITopic) context, (ITopic) params[0], (ITopic) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case PLAYER: {
			if (context instanceof IAssociationRole) {
				return doReadPlayer((IAssociationRole) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case VALUE: {
			if (context instanceof IDatatypeAware) {
				if (params.length == 0) {
					return doReadValue((IDatatypeAware) context);
				} else if (params.length == 1 && params[0] instanceof Class<?>) {
					return doReadValue((IDatatypeAware) context, (Class<?>) params[0]);
				}
			} else if (context instanceof IName) {
				return doReadValue((IName) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case DATATYPE: {
			if (context instanceof IDatatypeAware) {
				return doReadDataType((IDatatypeAware) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case ID: {
			return doReadId(context);
		}
		case ITEM_IDENTIFIER: {
			return doReadItemIdentifiers(context);
		}
		case REIFICATION: {
			if (context instanceof ITopic) {
				return doReadReification((ITopic) context);
			} else if (context instanceof IReifiable) {
				return doReadReification((IReifiable) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case REVISION_BEGIN: {
			if (context == null && params.length == 1 && params[0] instanceof IRevision) {
				return doReadRevisionBegin((IRevision) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case REVISION_END: {
			if (context == null && params.length == 1 && params[0] instanceof IRevision) {
				return doReadRevisionEnd((IRevision) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case NEXT_REVISION: {
			if (context == null && params.length == 1 && params[0] instanceof IRevision) {
				return doReadFutureRevision((IRevision) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case PREVIOUS_REVISION: {
			if (context == null && params.length == 1 && params[0] instanceof IRevision) {
				return doReadPreviousRevision((IRevision) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case CHANGESET: {
			if (context == null && params.length == 1 && params[0] instanceof IRevision) {
				return doReadChangeSet((IRevision) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case SCOPE: {
			if (context instanceof IScopable) {
				return doReadScope((IScopable) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case ASSOCIATION: {
			if (context instanceof ITopic) {
				if (params.length == 0) {
					return doReadAssociation((ITopic) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadAssociation((ITopic) context, (ITopic) params[0]);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadAssociation((ITopic) context, (IScope) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof IScope) {
					return doReadAssociation((ITopic) context, (ITopic) params[0], (IScope) params[1]);
				}
			} else if (context instanceof ITopicMap) {
				if (params.length == 0) {
					return doReadAssociation((ITopicMap) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadAssociation((ITopicMap) context, (ITopic) params[0]);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadAssociation((ITopicMap) context, (IScope) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof IScope) {
					return doReadAssociation((ITopicMap) context, (ITopic) params[0], (IScope) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case CHARACTERISTICS: {
			if (context instanceof ITopic) {
				if (params.length == 0) {
					return doReadCharacteristics((ITopic) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadCharacteristics((ITopic) context, (ITopic) params[0]);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadCharacteristics((ITopic) context, (IScope) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof IScope) {
					return doReadCharacteristics((ITopic) context, (ITopic) params[0], (IScope) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case NAME: {
			if (context instanceof ITopic) {
				if (params.length == 0) {
					return doReadNames((ITopic) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadNames((ITopic) context, (ITopic) params[0]);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadNames((ITopic) context, (IScope) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof IScope) {
					return doReadNames((ITopic) context, (ITopic) params[0], (IScope) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case OCCURRENCE: {
			if (context instanceof ITopic) {
				if (params.length == 0) {
					return doReadOccurrences((ITopic) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadOccurrences((ITopic) context, (ITopic) params[0]);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadOccurrences((ITopic) context, (IScope) params[0]);
				} else if (params.length == 2 && params[0] instanceof ITopic && params[1] instanceof IScope) {
					return doReadOccurrences((ITopic) context, (ITopic) params[0], (IScope) params[1]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case VARIANT: {
			if (context instanceof IName) {
				if (params.length == 0) {
					return doReadVariants((IName) context);
				} else if (params.length == 1 && params[0] instanceof IScope) {
					return doReadVariants((IName) context, (IScope) params[0]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case TYPE: {
			if (context instanceof ITopic) {
				return doReadTypes((ITopic) context);
			} else if (context instanceof ITypeable) {
				return doReadType((ITypeable) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case SUPERTYPE: {
			if (context instanceof ITopic) {
				return doReadSuptertypes((ITopic) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case SUBJECT_IDENTIFIER: {
			if (context instanceof ITopic) {
				return doReadSubjectIdentifiers((ITopic) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case SUBJECT_LOCATOR: {
			if (context instanceof ITopic) {
				return doReadSubjectLocators((ITopic) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case TOPIC: {
			if (context instanceof ITopicMap) {
				if (params.length == 0) {
					return doReadTopics((ITopicMap) context);
				} else if (params.length == 1 && params[0] instanceof ITopic) {
					return doReadTopics((ITopicMap) context, (ITopic) params[0]);
				}
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_ID: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof String) {
				return doReadConstruct((ITopicMap) context, (String) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_ITEM_IDENTIFER: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doReadConstruct((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_SUBJECT_IDENTIFER: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doReadTopicBySubjectIdentifier((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case BY_SUBJECT_LOCATOR: {
			if (context instanceof ITopicMap && params.length == 1 && params[0] instanceof ILocator) {
				return doReadTopicBySubjectLocator((ITopicMap) context, (ILocator) params[0]);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		case LOCATOR: {
			if (context instanceof ITopicMap) {
				return doReadLocator((ITopicMap) context);
			}
			throw new OperationSignatureException(context, paramType, params);
		}
		}
		throw new OperationSignatureException(context, paramType, params);
	}

	/**
	 * Read the association played by the given topic
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopic t) throws TopicMapStoreException;

	/**
	 * Read the associations played by the given topic and being typed by the
	 * given type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Read all scoped associations played by the given topic and being typed by
	 * the given type
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopic t, ITopic type, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all scoped associations played by the given topic
	 * 
	 * @param t
	 *            the topic
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopic t, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all associations of the topic map.
	 * 
	 * @param tm
	 *            the topic map
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopicMap tm) throws TopicMapStoreException;

	/**
	 * Read all associations of the topic map being typed by the given type.
	 * 
	 * @param tm
	 *            the topic map
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopicMap tm, ITopic type) throws TopicMapStoreException;

	/**
	 * Read all scoped associations of the topic map being typed by the given
	 * type.
	 * 
	 * @param tm
	 *            the topic map
	 * @param type
	 *            the type
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopicMap tm, ITopic type, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all scoped associations of the topic map.
	 * 
	 * @param tm
	 *            the topic map
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociation> doReadAssociation(ITopicMap tm, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all characteristics if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ICharacteristics> doReadCharacteristics(ITopic t) throws TopicMapStoreException;

	/**
	 * Read all characteristics if the given topic being typed by the given
	 * type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ICharacteristics> doReadCharacteristics(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Read all scoped characteristics if the given topic being typed by the
	 * given type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ICharacteristics> doReadCharacteristics(ITopic t, ITopic type, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all scoped characteristics if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ICharacteristics> doReadCharacteristics(ITopic t, IScope scope) throws TopicMapStoreException;

	/**
	 * Read the construct identified by the given id.
	 * 
	 * @param t
	 *            the topic map
	 * @param id
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IConstruct doReadConstruct(ITopicMap t, String id) throws TopicMapStoreException;

	/**
	 * Read the construct identified by the given item-identifier.
	 * 
	 * @param t
	 *            the topic map
	 * @param itemIdentifier
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IConstruct doReadConstruct(ITopicMap t, ILocator itemIdentifier) throws TopicMapStoreException;

	/**
	 * Read the data type of the given data-type-aware
	 * 
	 * @param d
	 *            the data-type-aware
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ILocator doReadDataType(IDatatypeAware d) throws TopicMapStoreException;

	/**
	 * Read the id of the given construct
	 * 
	 * @param c
	 *            the construct
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract String doReadId(IConstruct c) throws TopicMapStoreException;

	/**
	 * Read all item-identifiers of the given construct
	 * 
	 * @param c
	 *            the construct
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ILocator> doReadItemIdentifiers(IConstruct c) throws TopicMapStoreException;

	/**
	 * Read the base locator of the given topic map.
	 * 
	 * @param t
	 *            the topic map
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ILocator doReadLocator(ITopicMap t) throws TopicMapStoreException;

	/**
	 * Read all names if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IName> doReadNames(ITopic t) throws TopicMapStoreException;

	/**
	 * Read all names if the given topic being typed by the given type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IName> doReadNames(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Read all scoped names if the given topic being typed by the given type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IName> doReadNames(ITopic t, ITopic type, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all scoped names if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IName> doReadNames(ITopic t, IScope scope) throws TopicMapStoreException;

	/**
	 * Read the previous revision of the revision.
	 * 
	 * @param r
	 *            the revision
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IRevision doReadFutureRevision(IRevision r) throws TopicMapStoreException;

	/**
	 * Read all occurrences if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IOccurrence> doReadOccurrences(ITopic t) throws TopicMapStoreException;

	/**
	 * Read all occurrence if the given topic being typed by the given type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IOccurrence> doReadOccurrences(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Read all scoped occurrence if the given topic being typed by the given
	 * type.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IOccurrence> doReadOccurrences(ITopic t, ITopic type, IScope scope) throws TopicMapStoreException;

	/**
	 * Read all scoped occurrence if the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IOccurrence> doReadOccurrences(ITopic t, IScope scope) throws TopicMapStoreException;

	/**
	 * Read the player of the given role
	 * 
	 * @param role
	 *            the role
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doReadPlayer(IAssociationRole role) throws TopicMapStoreException;

	/**
	 * Read the next revision of the revision.
	 * 
	 * @param r
	 *            the revision
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IRevision doReadPreviousRevision(IRevision r) throws TopicMapStoreException;

	/**
	 * Read the reified item of the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IReifiable doReadReification(ITopic t) throws TopicMapStoreException;

	/**
	 * Read the reifier of the reified construct
	 * 
	 * @param r
	 *            the reified item
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doReadReification(IReifiable r) throws TopicMapStoreException;

	/**
	 * Read the time-stamp of the revision begin.
	 * 
	 * @param r
	 *            the revision
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Calendar doReadRevisionBegin(IRevision r) throws TopicMapStoreException;

	/**
	 * Read the time-stamp of the revision end.
	 * 
	 * @param r
	 *            the revision
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Calendar doReadRevisionEnd(IRevision r) throws TopicMapStoreException;

	/**
	 * Read the change set of a revision.
	 * 
	 * @param r
	 *            the revision
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Changeset doReadChangeSet(IRevision r) throws TopicMapStoreException;

	/**
	 * Read all association role items of the given association
	 * 
	 * @param association
	 *            the association
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociationRole> doReadRoles(IAssociation association) throws TopicMapStoreException;

	/**
	 * Read all association role items of the given association and the given
	 * type.
	 * 
	 * @param association
	 *            the association
	 * @param type
	 *            the role type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociationRole> doReadRoles(IAssociation association, ITopic type) throws TopicMapStoreException;

	/**
	 * Read the played roles of the given topic.
	 * 
	 * @param player
	 *            the role player
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociationRole> doReadRoles(ITopic player) throws TopicMapStoreException;

	/**
	 * Read the played roles of the given topic and being typed by the given
	 * type
	 * 
	 * @param player
	 *            the player
	 * @param type
	 *            the role type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociationRole> doReadRoles(ITopic player, ITopic type) throws TopicMapStoreException;

	/**
	 * Read the played roles of the given topic and being typed by the given
	 * type
	 * 
	 * @param player
	 *            the player
	 * @param type
	 *            the role type
	 * @param assocType
	 *            the association type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IAssociationRole> doReadRoles(ITopic player, ITopic type, ITopic assocType) throws TopicMapStoreException;

	/**
	 * Read all role types of the given association
	 * 
	 * @param association
	 *            the association
	 * @return a set containing all role types
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ITopic> doReadRoleTypes(IAssociation association) throws TopicMapStoreException;

	/**
	 * Read all subject-identifiers of the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ILocator> doReadSubjectIdentifiers(ITopic t) throws TopicMapStoreException;

	/**
	 * Read all subject-locators of the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ILocator> doReadSubjectLocators(ITopic t) throws TopicMapStoreException;

	/**
	 * Read all super types of the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ITopic> doReadSuptertypes(ITopic t) throws TopicMapStoreException;

	/**
	 * Read the topic identified by the given subject-identifier.
	 * 
	 * @param t
	 *            the topic map
	 * @param subjectIdentifier
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doReadTopicBySubjectIdentifier(ITopicMap t, ILocator subjectIdentifier) throws TopicMapStoreException;

	/**
	 * Read the topic identified by the given subject-locator.
	 * 
	 * @param t
	 *            the topic map
	 * @param subjectLocator
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doReadTopicBySubjectLocator(ITopicMap t, ILocator subjectLocator) throws TopicMapStoreException;

	/**
	 * Return all topics of the topic map.
	 * 
	 * @param t
	 *            the topic map
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ITopic> doReadTopics(ITopicMap t) throws TopicMapStoreException;

	/**
	 * Return all topics of the topic map being typed by the given type.
	 * 
	 * @param t
	 *            the topic map
	 * @param type
	 *            the type
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ITopic> doReadTopics(ITopicMap t, ITopic type) throws TopicMapStoreException;

	/**
	 * Read the type of the given typed item.
	 * 
	 * @param typed
	 *            the typed
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract ITopic doReadType(ITypeable typed) throws TopicMapStoreException;

	/**
	 * Read all types of the given type.
	 * 
	 * @param t
	 *            the topic
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<ITopic> doReadTypes(ITopic t) throws TopicMapStoreException;

	/**
	 * Read the scope of the given scoped item
	 * 
	 * @param s
	 *            the scoped item
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract IScope doReadScope(IScopable s) throws TopicMapStoreException;

	/**
	 * Read the value of the topic name.
	 * 
	 * @param n
	 *            the name
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Object doReadValue(IName n) throws TopicMapStoreException;

	/**
	 * Read the value of the data-type-aware.
	 * 
	 * @param t
	 *            the data-type-aware
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Object doReadValue(IDatatypeAware t) throws TopicMapStoreException;

	/**
	 * Read the value of the data-type-aware and cast them to the given type
	 * 
	 * @param <T>
	 *            the type of the value
	 * @param t
	 *            the data-type-aware
	 * @param type
	 *            the type of the value
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract <T extends Object> T doReadValue(IDatatypeAware t, Class<T> type) throws TopicMapStoreException;

	/**
	 * Read the variants of the given name.
	 * 
	 * @param n
	 *            the name
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IVariant> doReadVariants(IName n) throws TopicMapStoreException;

	/**
	 * Read the variants of the given name in the given scope.
	 * 
	 * @param n
	 *            the name
	 * @param scope
	 *            the scope
	 * @return the read information
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract Set<IVariant> doReadVariants(IName n, IScope scope) throws TopicMapStoreException;

	// ********************
	// * REMOVE OPERATION *
	// ********************

	/**
	 * {@inheritDoc}
	 */
	public void doRemove(IConstruct context, TopicMapStoreParameterType paramType, Object... params) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		if (isReadOnly()) {
			throw new UnmodifyableStoreException("Remove operation not supported by read-only stores!");
		}
		switch (paramType) {
		case ITEM_IDENTIFIER: {
			if (params.length == 1 && params[0] instanceof ILocator) {
				doRemoveItemIdentifier(context, (ILocator) params[0]);
			}
		}
			break;
		case SCOPE: {
			if (context instanceof IScopable && params.length == 1 && params[0] instanceof ITopic) {
				doRemoveScope((IScopable) context, (ITopic) params[0]);
			}
		}
			break;
		case SUBJECT_IDENTIFIER: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ILocator) {
				doRemoveSubjectIdentifier((ITopic) context, (ILocator) params[0]);
			}
		}
			break;
		case SUBJECT_LOCATOR: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ILocator) {
				doRemoveSubjectLocator((ITopic) context, (ILocator) params[0]);
			}
		}
			break;
		case SUPERTYPE: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ITopic) {
				doRemoveSupertype((ITopic) context, (ITopic) params[0]);
			}
		}
			break;
		case TYPE: {
			if (context instanceof ITopic && params.length == 1 && params[0] instanceof ITopic) {
				doRemoveType((ITopic) context, (ITopic) params[0]);
			}
		}
			break;
		default: {
			throw new OperationSignatureException(context, paramType, params);
		}
		}
	}

	/**
	 * Remove the given item identifier from the given construct.
	 * 
	 * @param c
	 * @param itemIdentifier
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveItemIdentifier(IConstruct c, ILocator itemIdentifier) throws TopicMapStoreException;

	/**
	 * Remove the theme from the given scoped item.
	 * 
	 * @param s
	 *            the scoped item
	 * @param theme
	 *            the theme
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveScope(IScopable s, ITopic theme) throws TopicMapStoreException;

	/**
	 * Remove the subject-identifier from the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param subjectIdentifier
	 *            the subject-identifier
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveSubjectIdentifier(ITopic t, ILocator subjectIdentifier) throws TopicMapStoreException;

	/**
	 * Remove the subject-locator from the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param subjectLocator
	 *            the subject-locator
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveSubjectLocator(ITopic t, ILocator subjectLocator) throws TopicMapStoreException;

	/**
	 * Remove the type from the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveSupertype(ITopic t, ITopic type) throws TopicMapStoreException;

	/**
	 * Remove the super type from the given topic.
	 * 
	 * @param t
	 *            the topic
	 * @param type
	 *            the type
	 * @throws TopicMapStoreException
	 *             thrown if operation fails
	 */
	protected abstract void doRemoveType(ITopic t, ITopic type) throws TopicMapStoreException;

	// ********************
	// * REMOVE CONSTRUCT *
	// ********************

	/**
	 * {@inheritDoc}
	 */
	public void doRemove(IConstruct context, boolean cascade) throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		if (isReadOnly()) {
			throw new UnmodifyableStoreException("Remove operation not supported by read-only stores!");
		}
		if (context instanceof ITopicMap) {
			doRemoveTopicMap((ITopicMap) context, cascade);
			close();
		} else if (context instanceof ITopic) {
			doRemoveTopic((ITopic) context, cascade);
		} else if (context instanceof IName) {
			doRemoveName((IName) context, cascade);
		} else if (context instanceof IOccurrence) {
			doRemoveOccurrence((IOccurrence) context, cascade);
		} else if (context instanceof IAssociation) {
			doRemoveAssociation((IAssociation) context, cascade);
		} else if (context instanceof IAssociationRole) {
			doRemoveRole((IAssociationRole) context, cascade);
		} else if (context instanceof IVariant) {
			doRemoveVariant((IVariant) context, cascade);
		} else {
			throw new TopicMapStoreException("Unknown construct type to remove '" + context.getClass() + "'.");
		}
	}

	/**
	 * Remove the topic map.
	 * 
	 * @param topicMap
	 *            the topic map
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveTopicMap(ITopicMap topicMap, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the topic.
	 * 
	 * @param topic
	 *            the topic
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveTopic(ITopic topic, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the name.
	 * 
	 * @param name
	 *            the name
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveName(IName name, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the occurrence.
	 * 
	 * @param occurrence
	 *            the occurrence
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveOccurrence(IOccurrence occurrence, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the association.
	 * 
	 * @param association
	 *            the association
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveAssociation(IAssociation association, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the association role.
	 * 
	 * @param role
	 *            the role
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveRole(IAssociationRole role, boolean cascade) throws TopicMapStoreException;

	/**
	 * Remove the variant.
	 * 
	 * @param variant
	 *            the variant
	 * @param cascade
	 *            flag indicates if the dependent construct should removed too
	 * @throws TopicMapStoreException
	 */
	protected abstract void doRemoveVariant(IVariant variant, boolean cascade) throws TopicMapStoreException;

	/**
	 * {@inheritDoc}
	 */
	public void connect() throws TopicMapStoreException {
		if (this.topicMapSystem == null) {
			throw new TopicMapStoreException("Store is not bind to any topic map system instance!");
		}
		if (this.topicMap == null) {
			throw new TopicMapStoreException("Store is not bind to any topic map instance!");
		}
		if (isConnected()) {
			throw new TopicMapStoreException("Connection is already established");
		}
		connected = true;

		Object maximum = topicMapSystem.getProperty(TopicMapStoreProperty.THREADPOOL_MAXIMUM);
		int max = 10;
		if (maximum != null) {
			try {
				max = Integer.parseInt(maximum.toString());
			} catch (NumberFormatException e) {
				// NOTHING TO DO
			}
		}
		this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(max);
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws TopicMapStoreException {
		if (!isConnected()) {
			throw new TopicMapStoreException("Connection is not established");
		}
		topicMapSystem.removeTopicMap(((ITopicMap) topicMap).getLocator());
		connected = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isConnected() {
		return connected;
	}

	/**
	 * Returns the topic map
	 * 
	 * @return the topicMap
	 */
	public ITopicMap getTopicMap() {
		return topicMap;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTopicMap(ITopicMap topicMap) throws TopicMapStoreException {
		if (this.topicMap != null) {
			throw new TopicMapStoreException("Store is already bind to an other topic map instance!");
		}
		this.topicMap = topicMap;
	}

	/**
	 * Returns the parent topic map system
	 * 
	 * @return the topicMapSystem
	 */
	public ITopicMapSystem getTopicMapSystem() {
		return topicMapSystem;
	}

	/**
	 * Transform the given array to a set of the specific type.
	 * 
	 * @param <T>
	 *            the type
	 * @param array
	 *            the array
	 * @return the transformed set
	 */
	private static Collection<ITopic> convert(Topic[] array) {
		Set<ITopic> target = HashUtil.getHashSet();
		for (Topic o : array) {
			target.add((ITopic) o);
		}
		return target;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isReadOnly() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.READ_ONLY_SYSTEM);
		} catch (FeatureNotRecognizedException e) {
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean supportRevisions() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.SUPPORT_HISTORY);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method checks if the feature 'http://tmapi.org/features/automerge/' for
	 * automatic merging is set.
	 * 
	 * @return <code>true</code> if automatic merging is enabled,
	 *         <code>false</code> otherwise.
	 */
	public boolean doAutomaticMerging() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.AUTOMATIC_MERGING);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method checks if the feature
	 * 'http://tmapi.org/features/merge/byTopicName/' for merging topics by name
	 * is set.
	 * 
	 * @return <code>true</code> if merging by name is enabled,
	 *         <code>false</code> otherwise.
	 */
	public boolean doMergingByTopicName() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.MERGING_SUPPORT_FEATURE_BY_TOPIC_NAME);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method checks if the feature for supporting type-instance-associations as
	 * a type relation is set.
	 * 
	 * @return <code>true</code> if the associations should recognized,
	 *         <code>false</code> otherwise.
	 */
	public boolean recognizingTypeInstanceAssociation() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.TOPIC_MAPS_TYPE_INSTANCE_ASSOCIATION);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method checks if the feature for supporting
	 * supertype-subtype-associations as a supertype relation is set.
	 * 
	 * @return <code>true</code> if the associations should recognized,
	 *         <code>false</code> otherwise.
	 */
	public boolean recognizingSupertypeSubtypeAssociation() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.TOPIC_MAPS_SUPERTYPE_SUBTYPE_ASSOCIATION);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method returns the value of the property indicates if a type-hierarchy
	 * should be materialized as association.
	 * 
	 * @return <code>true</code> if the type-hierarchy should be materialized as
	 *         association, <code>false</code> otherwise.
	 */
	public boolean typeHiearchyAsAssociation() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.TYPEHIERARCHY_AS_ASSOCIATION);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method returns checks if the deletion constraint contains the constraint,
	 * that topics used as reifier cannot be removed until the reification was
	 * destroyed.
	 * 
	 * @see TopicMapStoreProperty#DELETION_CONSTRAINTS_REIFICATION
	 * 
	 * @return <code>true</code> if a topic cannot be removed if it is used as
	 *         reifier , <code>false</code> otherwise.
	 */
	public boolean isReificationDeletionRestricted() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.DELETION_CONSTRAINTS_REIFICATION);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransactable() {
		try {
			return topicMapSystem.getFeature(FeatureStrings.SUPPORT_TRANSACTION);
		} catch (FeatureNotRecognizedException e) {
			return false;
		}
	}

	/**
	 * Method called by the topic map store to notify all topic map listeners
	 * registered for the given topic map
	 * 
	 * @param event
	 *            the event type
	 * @param notifier
	 *            the construct changed
	 * @param newValue
	 *            the new value
	 * @param oldValue
	 *            the old value
	 */
	protected void notifyListeners(TopicMapEventType event, IConstruct notifier, Object newValue, Object oldValue) {
		if (listeners != null) {
			addTaskToThreadPool(new EventNotifier(listeners, event, notifier, newValue, oldValue));
		}
	}

	/**
	 * Adding the given task to the internal thread pool of the topic map store.
	 * 
	 * @param task
	 *            the task to add
	 */
	protected void addTaskToThreadPool(Runnable task) {
		getThreadPool().execute(task);
	}

	/**
	 * Returns the internal thread pool instance
	 * 
	 * @return the threadPool
	 */
	protected final ThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setTopicMapSystem(ITopicMapSystem topicMapSystem) {
		if (isConnected()) {
			throw new TopicMapStoreException("Store already connected, topic map system cannot changed!");
		}
		this.topicMapSystem = topicMapSystem;
	}

}
