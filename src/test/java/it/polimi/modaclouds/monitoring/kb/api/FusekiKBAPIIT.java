/**
 * Copyright 2014 deib-polimi
 * Contact: deib-polimi <marco.miglierina@polimi.it>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package it.polimi.modaclouds.monitoring.kb.api;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FusekiKBAPIIT {

	private FusekiKBAPI kb;

	@Before
	public void setUp() {
		kb = new FusekiKBAPI("http://localhost:3030/modaclouds/kb");
		kb.clearAll();
	}

	@After
	public void tearDown() {
		kb.clearAll();
	}
	
	@Test
	public void shouldFindWhatIPut() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		MyEntity entity2 = new MyEntity();
		entity1.setId("entity1");
		entity2.setId("entity2");
		entity1.addElementToList(entity2);
		entity1.addElementToMap("property", entity2);
		entity1.addElementToSet(entity2);
		entity1.setEntity(entity2);
		entity1.setString("provaString");
		kb.add(entity1, "id");
		kb.add(entity2, "id");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertTrue(retrievedEntity1 instanceof MyEntity);
		assertEquals(entity1.getId(), ((MyEntity) retrievedEntity1).getId());
		assertEquals(entity1.getEntity().getId(), ((MyEntity) retrievedEntity1)
				.getEntity().getId());
		assertEquals(entity1.getMap().get("property").getId(),
				((MyEntity) retrievedEntity1).getMap().get("property").getId());
	}

	@Test
	public void shouldNotFindWhatIDidNotPut() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		MyEntity entity2 = new MyEntity();
		entity1.setId("entity1");
		entity2.setId("entity2");
		entity1.setEntity(entity2);
		kb.add(entity1, "id");
		assertTrue(((MyEntity) kb.getEntityById("entity1", "id")).getEntity() == null);
	}

	@Test
	public void shouldWorksFineWithRecursivity() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.setEntity(entity1);
		entity1.addElementToList(entity1);
		kb.add(entity1, "id");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertEquals(entity1.getEntity().getId(), ((MyEntity) retrievedEntity1)
				.getEntity().getId());
		assertEquals(retrievedEntity1,
				((MyEntity) retrievedEntity1).getEntity());
		assertEquals(retrievedEntity1, ((MyEntity) retrievedEntity1).getList()
				.get(0));
	}
	
	@Test
	public void kbShouldBeEmptyInTheBegininning(){
		assertTrue(kb.datasetAccessor.getModel().isEmpty());
	}
	
	@Test
	public void shouldBeEmptyAfterRemove() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		MyEntity entity2 = new MyEntity();
		entity1.setId("entity1");
		entity2.setId("entity2");
		entity1.addElementToList(entity2);
		entity1.addElementToMap("property", entity2);
		entity1.addElementToSet(entity2);
		entity1.setEntity(entity2);
		entity1.setString("provaString");
		kb.add(entity1, "id");
		kb.add(entity2, "id");
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		kb.deleteEntitiesByPropertyValue("entity2", "id");
		assertTrue(kb.datasetAccessor.getModel().isEmpty());
	}
	
	@Test
	public void shouldDeleteOnlyTheDeletedEntity1() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		MyEntity entity2 = new MyEntity();
		entity1.setId("entity1");
		entity2.setId("entity2");
		entity1.addElementToList(entity2);
		entity1.addElementToMap("property", entity2);
		entity1.addElementToSet(entity2);
		entity1.setEntity(entity2);
		entity1.setString("provaString");
		kb.add(entity1, "id");
		kb.add(entity2, "id");
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		Object retrievedEntity2 = kb.getEntityById("entity2", "id");
		assertEquals(entity2.getId(), ((MyEntity) retrievedEntity2).getId());
		assertTrue(kb.getEntityById("entity1", "id") == null);
	}
	
	@Test
	public void shouldDeleteOnlyTheDeletedEntity2() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		MyEntity entity2 = new MyEntity();
		entity1.setId("entity1");
		entity2.setId("entity2");
		entity1.addElementToList(entity2);
		entity1.addElementToMap("property", entity2);
		entity1.addElementToSet(entity2);
		entity1.setEntity(entity2);
		entity1.setString("provaString");
		kb.add(entity1, "id");
		kb.add(entity2, "id");
		kb.deleteEntitiesByPropertyValue("entity2", "id");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertEquals(entity1.getId(), ((MyEntity) retrievedEntity1).getId());
		assertNull(((MyEntity) retrievedEntity1).getEntity());
		assertNull(((MyEntity) retrievedEntity1).getMap().get("property"));
		assertTrue(kb.getEntityById("entity2", "id") == null);
	}
	
	@Test
	public void shouldDeleteWithRecursivity() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.setEntity(entity1);
		entity1.addElementToList(entity1);
		kb.add(entity1, "id");
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		assertTrue(kb.datasetAccessor.getModel().isEmpty());
	}
	
	@Test
	public void shoudlOverwriteOnSameId() throws SerializationException,
			DeserializationException {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.setEntity(entity1);
		entity1.addElementToMap("someProp", entity1);
		kb.add(entity1, "id");
		MyEntity entity2 = new MyEntity();
		entity2.setId("entity1");
		entity2.setEntity(entity2);
		entity2.addElementToList(entity2);
		kb.add(entity2, "id");
		assertTrue(((MyEntity)kb.getEntityById("entity1", "id")).getMap().isEmpty());
		assertFalse(((MyEntity)kb.getEntityById("entity1", "id")).getList().isEmpty());
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		assertTrue(kb.datasetAccessor.getModel().isEmpty());
	}

}
