package fi.monkeyball.fi.monekyball.spring.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by juhof on 15/08/15.
 */
public class CapturingRowMapperTest {

    private ResultSet resultSetMock;

    @Before
    public void setUp() throws Exception {
        resultSetMock = mock(ResultSet.class);
    }

    @Test
    public void assertCallsMapBaseObject() throws SQLException {
        final AtomicBoolean mapBaseObjectCalled = new AtomicBoolean(false);
        CapturingRowMapper capturingRowMapper = new CapturingRowMapper<TestDomainObject>("field1") {
            @Override
            public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                mapBaseObjectCalled.set(true);
                assertSame("Unexpected result set reference in mapBaseObject", resultSetMock, resultSet);
                return new TestDomainObject();
            }
        };
        capturingRowMapper.mapRow(this.resultSetMock, 0);
        assertTrue("Calling mapRow() but mapBaseObject was not called", mapBaseObjectCalled.get());
    }

    @Test
    public void assertKeepsIndexCorrect() throws Exception {
        CapturingRowMapper capturingRowMapper = new CapturingRowMapper<TestDomainObject>("field1") {
            private int expectedNextId = 0;
            @Override
            public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                assertTrue("Excpecting " + expectedNextId + " but was " + i, i == expectedNextId);
                expectedNextId++;
                return new TestDomainObject();
            }
        };

        for (int i = 0; i < 1000; i++) {
            capturingRowMapper.mapRow(this.resultSetMock, i);
        }

    }

    @Test(expected = CapturingRowMapperException.class)
    public void assertThrowsIfMapBaseObjectReturnsNull() throws Exception {
        CapturingRowMapper capturingRowMapper = new CapturingRowMapper("field1") {
            @Override
            public Object mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                return null;
            }
        };
        capturingRowMapper.mapRow(this.resultSetMock, 0);
    }

    @Test
    public void assertRetursSameExactObjectReturnedFromMapBaseObject() throws SQLException {

        final TestDomainObject testObject = new TestDomainObject();

        CapturingRowMapper<TestDomainObject> capturingRowMapper = new CapturingRowMapper<TestDomainObject>("field1") {
            @Override
            public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                return testObject;
            }
        };
        TestDomainObject o = capturingRowMapper.mapRow(this.resultSetMock, 0);

        assertSame(testObject, o);
    }

    @Test
    public void testCaptureDefinedFieldsFromResultSet() throws Exception {

        when(this.resultSetMock.getObject("field1")).thenReturn("Value of field 1");
        when(this.resultSetMock.getObject("field2")).thenReturn(1);
        when(this.resultSetMock.getObject("field3")).thenReturn(UUID.nameUUIDFromBytes("juuh".getBytes()));

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>("field1", "field2", "field3") {

                    @Override
            public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                return new TestDomainObject();
            }
        };
        TestDomainObject testDomainObject = capturingRowMapper.mapRow(this.resultSetMock, 0);

        assertEquals("Value of field 1", capturingRowMapper.captured(testDomainObject, "field1", String.class));
        assertEquals(new Integer(1), capturingRowMapper.captured(testDomainObject, "field2", Integer.class));
        assertEquals(UUID.nameUUIDFromBytes("juuh".getBytes()), capturingRowMapper.captured(testDomainObject, "field3", UUID.class));
    }

    @Test(expected = FieldIsNotCapturedException.class)
    public void assertThrowsIfAskingNonCapturedField() throws Exception {

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>() {
                    @Override
                    public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                        return new TestDomainObject();
                    }
                };
        TestDomainObject testDomainObject = capturingRowMapper.mapRow(this.resultSetMock, 0);

        capturingRowMapper.captured(testDomainObject, "field1", String.class);
    }

    @Test(expected = FieldIsNotExpectedTypeException.class)
    public void assertThrowsIfCapturedFieldIsNotAskedType() throws Exception {

        when(this.resultSetMock.getObject("field1")).thenReturn("Value of field 1");

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>("field1") {
                    @Override
                    public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                        return new TestDomainObject();
                    }
                };
        TestDomainObject testDomainObject = capturingRowMapper.mapRow(this.resultSetMock, 0);

        capturingRowMapper.captured(testDomainObject, "field1", Integer.class);
    }

    @Test
    public void testMappingMultipleRows() throws SQLException {

        when(this.resultSetMock.getObject("field1"))
                .thenReturn("value for first object")
                .thenReturn("value for second object");

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>("field1") {
                    @Override
                    public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                        return new TestDomainObject();
                    }
                };
        TestDomainObject firstMappedObject = capturingRowMapper.mapRow(this.resultSetMock, 0);
        TestDomainObject secondsMappedObject = capturingRowMapper.mapRow(this.resultSetMock, 1);

        assertEquals("value for first object", capturingRowMapper.captured(firstMappedObject, "field1", String.class));
        assertEquals("value for second object", capturingRowMapper.captured(secondsMappedObject, "field1", String.class));
    }

    @Test
    public void testCapturingMultipleFieldsFromMultipleRows() throws SQLException {

        when(this.resultSetMock.getObject("field1"))
                .thenReturn("value for first object")
                .thenReturn("value for second object")
                .thenReturn("value for third object");

        when(this.resultSetMock.getObject("field2"))
                .thenReturn(1)
                .thenReturn(2)
                .thenReturn(3);

        when(this.resultSetMock.getObject("field3"))
                .thenReturn(UUID.nameUUIDFromBytes("field1".getBytes()))
                .thenReturn(UUID.nameUUIDFromBytes("field2".getBytes()))
                .thenReturn(UUID.nameUUIDFromBytes("field3".getBytes()));

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>("field1", "field2", "field3") {
                    @Override
                    public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                        return new TestDomainObject();
                    }
                };

        TestDomainObject firstMappedObject = capturingRowMapper.mapRow(this.resultSetMock, 0);
        TestDomainObject secondMappedObject = capturingRowMapper.mapRow(this.resultSetMock, 1);
        TestDomainObject thirdMappedObject = capturingRowMapper.mapRow(this.resultSetMock, 2);

        assertEquals("value for first object", capturingRowMapper.captured(firstMappedObject, "field1", String.class));
        assertEquals("value for second object", capturingRowMapper.captured(secondMappedObject, "field1", String.class));
        assertEquals("value for third object", capturingRowMapper.captured(thirdMappedObject, "field1", String.class));

        assertEquals(new Integer(1), capturingRowMapper.captured(firstMappedObject, "field2", Integer.class));
        assertEquals(new Integer(2), capturingRowMapper.captured(secondMappedObject, "field2", Integer.class));
        assertEquals(new Integer(3), capturingRowMapper.captured(thirdMappedObject, "field2", Integer.class));

        assertEquals(UUID.nameUUIDFromBytes("field1".getBytes()), capturingRowMapper.captured(firstMappedObject, "field3", UUID.class));
        assertEquals(UUID.nameUUIDFromBytes("field2".getBytes()), capturingRowMapper.captured(secondMappedObject, "field3", UUID.class));
        assertEquals(UUID.nameUUIDFromBytes("field3".getBytes()), capturingRowMapper.captured(thirdMappedObject, "field3", UUID.class));

    }

    @Test
    public void testCapturingFromHugeResultSet() throws Exception {

        final int SIZE_OF_RESULT_SET = 10000;

        when(this.resultSetMock.getObject(anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                return count++;
            }
        });

        CapturingRowMapper<TestDomainObject> capturingRowMapper =
                new CapturingRowMapper<TestDomainObject>("field1") {
                    @Override
                    public TestDomainObject mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                        return new TestDomainObject();
                    }
                };

        List<TestDomainObject> mappedObjects = new LinkedList<>();

        for (int i = 0; i < SIZE_OF_RESULT_SET; i++) {
            mappedObjects.add(capturingRowMapper.mapRow(this.resultSetMock, i));
        }

        for (int i = 0; i < SIZE_OF_RESULT_SET; i++) {
            TestDomainObject mappedObjectInIndex = mappedObjects.get(i);
            assertEquals(new Integer(i), capturingRowMapper.captured(mappedObjectInIndex, "field1", Integer.class));
        }
    }

    public static class TestDomainObject {}
}