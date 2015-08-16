# spring-jdbc-capturing-rowmapper
It's a base RowMapper which captures defined fields for later use.

## Rationale

ORM is a pain in the ass. This solves one simple problem in object-relation-mapping pretty nicely.

Consider having Person which has Nationality.

Person.java
```java
public class Person {
    private String name;
    private int age;
    private Country country;
    // Getters and setters omitted
  }
```

Country.java
```java
public class Country {

    private Integer id;
    private String name;
    // Getters and setters omitted
}
```

It's nice java-model. Now, you probably have daos for both. PersonDAO depends on CountryDAO 
(because Person has nationality) and NationalityDAO should be totally unaware about Persons because 
countries can exist in various other contexts as well.

Let's build sql-schema.
```sql
CREATE TABLE COUNTRY (id INTEGER PRIMARY KEY, name VARCHAR);
CREATE TABLE PERSONS (
  id INTEGER PRIMARY KEY,
  name VARCHAR,
  age INTEGER,
  id_country INTEGER NOT NULL REFERENCES PUBLIC.COUNTRY(id));
```
Nice.

Let's implement getPersons() to PersonDAO.

You could do this because anonymous RowMapper can refer to countryDao instance in PersonDAO. 

```java
@Repository
public class PersonDAO {

    @Autowired
    private JdbcTemplate jt;

    @Autowired
    private CountryDAO countryDao;
    
    public List<Person> getPersons() {
        List<Person> persons = jt.query("SELECT * FROM persons", new RowMapper<Person>() {
            public Person mapRow(ResultSet resultSet, int i) throws SQLException {
                Person person = new Person();
                person.setName(resultSet.getString("name"));
                person.setAge(resultSet.getInt("age"));
                // ERROR: this borrows another connection from the pool
                person.setCountry(countryDao.getCountry(resultSet.getString("id_country")));
                return person;
            }
        });
        return persons;
    }
}
```
This seems quite ok at first. While mapping rows fetch persons country from countryDao. 
Problem is that because current connection is reserved while mapping, it has to borrow ANOTHER database connection from
pool or create one. This is highly unwanted because it can lead to really weird errors and situations on production.

You can do this:
Person.java
```java
public class Person {
    private String name;
    private int age;
    private String countryId;
    private Country country;
    // Getters and setters omitted
  }
```
You read countryId to separate sort of "temporary" field while mapping rows in your domain object, and use that to populate Country field.
Now you have several problems. This is total load of bollocks because you want to keep your domain classes as clean
and simple as possible. Don't do something like this. EVER.

Good solution for this is to use natural sql-based solution. Joins.
```java
    public List<Person> getPersons() {
        List<Person> persons = jt.query("SELECT * FROM persons AS p JOIN country AS c ON p.id_country = c.id", new RowMapper<Person>() {
            public Person mapRow(ResultSet resultSet, int i) throws SQLException {
                Person person = new Person();
                person.setName(resultSet.getString("p.name"));
                person.setAge(resultSet.getInt("age"));
                Country country = new Country();
                country.setId(resultSet.getInteger("c.id"));
                country.setName(resultSet.getString("c.name"));
                person.setCountry(country);
                return person;
            }
        });
        return persons;
    }
```
This is OK if this is OK. If Country is not used anywhere else this probably is the best and simplest solution. But if Country is
used in variety of other places you have a problem.

## Solution

```java
    public List<Person> getPersons() {
        // Build stateful row mapper which captures id_country
        CapturingRowMapper<Person> capturingRowMapper = new CapturingRowMapper<Person>("id_country") {
            @Override
            public Person mapBaseObject(ResultSet resultSet, int i) throws SQLException {
                Person person = new Person();
                person.setName(resultSet.getString("name"));
                person.setAge(resultSet.getInt("age"));
                // No country mapping here
                return person;
            }
        };

        // Query for persons
        List<Person> persons = jt.query("SELECT * FROM persons", capturingRowMapper);

        // Populate countries using capturing row mapper.
        // Note the capturingRowMapper.captured()-call. It returns id_country which
        // was at the same row that person instance was created from.
        for (Person person : persons) {
            person.setNationality(
                    nationalityDao.getNationality(
                            capturingRowMapper.captured(person, "id_country", Integer.class)));
        }

        return persons;
    }
```

I think above is pretty ok solution for a single problem in orm. This base class can be used in static row mappers as well.

Other possible solution is to use database dto:s, but this leads to more code. From anonymous RowMappers one can place values to final AtomicReference value variables. This gets tricky when mapping multiple objects and it does not support static row-mapper classes.
