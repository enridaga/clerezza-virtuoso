# Virtuoso Storage bindings for Clerezza

TODO

## Install
First install the ext.virtuoso.jdbc bundle.

## Do tests
To activate tests, you must execute maven with the virtuoso-do-tests profile, for example:

 $ mvn test -Pvirtuoso-do-tests
 
By default, the tests will use the parameters configured in the pom.xml. 
To override them you can also do the following:

 $ mvn test -Pvirtuoso-do-tests -DargLine="-Dvirtuoso.password=mypassword -Dvirtuoso.port=1234"
 
You can configure the following parameters:
 * virtuoso.test (default is null, sets to true if you activate the 'virtuoso-do-tests' profile)
 * virtuoso.driver (default is 'virtuoso.jdbc4.Driver')
 * virtuoso.host (default is 'localhost')
 * virtuoso.port (default is '1111')
 * virtuoso.user (default is 'dba')
 * virtuoso.password (default is 'dba')