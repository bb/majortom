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
/**
 * 
 */
package de.topicmapslab.majortom.tests.revision;

import org.tmapi.core.Topic;
import org.tmapi.core.TopicMap;

import de.topicmapslab.majortom.model.core.IAssociation;
import de.topicmapslab.majortom.model.core.IAssociationRole;
import de.topicmapslab.majortom.model.core.ITopic;
import de.topicmapslab.majortom.model.index.IRevisionIndex;
import de.topicmapslab.majortom.model.revision.IRevision;
import de.topicmapslab.majortom.revision.core.ReadOnlyAssociation;
import de.topicmapslab.majortom.tests.MaJorToMTestCase;

/**
 * @author Sven Krosse
 * 
 */
public class TestAssociationRevisions extends MaJorToMTestCase {

	public void testRevision() throws Exception {				
		ITopic type = createTopic();
		ITopic theme = createTopic();
		ITopic reifier = createTopic();
		IAssociation association = (IAssociation) topicMap.createAssociation(type, theme);
		IAssociationRole role = (IAssociationRole) association.createRole(createTopic(), createTopic());
		IAssociationRole other = (IAssociationRole) association.createRole(createTopic(), createTopic());
		association.setReifier(reifier);

		association.remove();

		IRevisionIndex index = topicMap.getIndex(IRevisionIndex.class);
		index.open();

		IRevision revision = index.getLastRevision();
		assertEquals(4, revision.getChangeset().size());
		assertEquals(association, revision.getChangeset().get(3).getOldValue());
		assertTrue(revision.getChangeset().get(3).getOldValue() instanceof ReadOnlyAssociation);
		
		association = (IAssociation) revision.getChangeset().get(3).getOldValue();
		
		assertEquals(topicMap, association.getParent());
		assertEquals(type, association.getType());
		assertEquals(reifier, association.getReifier());
		assertEquals(2, association.getRoles().size());
		assertTrue(association.getRoles().contains(role));
		assertTrue(association.getRoles().contains(other));
	}
	
	public void testCreateAssociation() throws Exception {
		IRevisionIndex index = topicMap.getIndex(IRevisionIndex.class);
		index.open();
		assertNull(index.getFirstRevision());
		
		topicMap.getStore().enableRevisionManagement(false);
		ITopic type = createTopic();
		assertNull(index.getFirstRevision());
		
		topicMap.getStore().enableRevisionManagement(true);
		assertNull(index.getFirstRevision());		
		
		topicMap.createAssociation(type);
		assertNotNull(index.getFirstRevision());
		IRevision r = index.getFirstRevision();
		assertEquals(3, r.getChangeset().size());
		assertNull(r.getFuture());
		
	}
	
	public void testCreateAssociationByMergeIn() throws Exception {
		IRevisionIndex index = topicMap.getIndex(IRevisionIndex.class);
		index.open();
		assertNull(index.getFirstRevision());
		
		TopicMap tm = factory.newTopicMapSystem().createTopicMap("http://psi.second.com/");
		Topic type = tm.createTopic();		
		tm.createAssociation(type);
		
		topicMap.mergeIn(tm);
		assertNotNull(index.getFirstRevision());
		IRevision r = index.getFirstRevision();
		System.out.println(r.getChangeset());
		assertEquals(5, r.getChangeset().size());
		assertNull(r.getFuture());		
	}

}
