Sensor Data Service
===========================================
This repository is part of the bachelor thesis 'A Multimodal Mentoring Cockpit for Tutor Support'.
It is a las2peer Service, that enables the connection between [ARLEMPanel](https://github.com/rwth-acis/ARLEMPanel) database and Learning Locker.
Other related repositories for the bachelor thesis can be found here: [Mentoring Cockpit](https://github.com/rwth-acis/Mentoring-Cockpit)

Sensor data
-----------
Actions performed in the [ARLEMPanel](https://github.com/rwth-acis/ARLEMPanel) are stored by the [msfusion.net](https://github.com/rwth-acis/msfusion.net) context manager using [ARLEMServices](https://github.com/rizalishan/ARLEMServices) components.
The sensor data service connects directly to the database used by [ARLEMServices](https://github.com/rizalishan/ARLEMServices) and [ARLEMPanel](https://github.com/rwth-acis/ARLEMPanel) and selects all the necessary data for xAPI statements, that represent actions performed by persons.
Then the service creates the statements and offers a RESTful POST request to send them to the configured LRS instance.

Currently the statements include only a person and a action, that is performed.

To send the statements to the LRS:
```
POST <service-address>/sensor/sendData
```

Therefore, replace *service-address* with the address the service is running on.


Learning Locker configuration
--------------------------
In Learning Locker an LRS is configured under Settings > Store > Add new.
And under Settings > Client a corresponding client can be configured with the authentication.
Don't forget to bootstrap the service to a instance of the [learning-locker-service](https://github.com/rwth-acis/learning-locker-service)

Service setup
-------------
To set up the service configure the [property file](etc/i5.las2peer.services.sensorDataService.SensorDataService.properties) file with the database credentials and Learning Locker authentication.
```INI
lrsDomain = http://exampleDomain/data/xAPI/statements
lrsAuth = Basic exampleauth
mysqlUser = exampleuser
mysqlPassword = examplepass
mysqlHost = localhost
mysqlPort = 3306
mysqlDatabase = exampledb
```

Build
--------
Execute the following command on your shell:

```shell
ant jar 
```

Start
--------

To start the sensor-data service, follow the [Starting-A-las2peer-Network tutorial](https://github.com/rwth-acis/las2peer-Template-Project/wiki/Starting-A-las2peer-Network) and bootstrap your service to a [mobsos-data-processing service](https://github.com/rwth-acis/mobsos-data-processing/tree/bachelor-thesis-philipp-roytburg).

How to run using Docker
-------------------

First build the image:
```bash
docker build . -t learning-locker
```

Then you can run the image like this:

```bash
docker run -e LRS_DOMAIN=lrsDomain -e LRS_AUTH=lrsAuth -e MYSQL_USER=mysqlUser -e MYSQL_PASSWORD=mysqlPassword -e MYSQL_HOST=mysqlHost -e MYSQL_PORT=mysqlPort -e MYSQL_DATABASE=mysqlDatabase -p 9011:9011 mentoring-cockpit-service
```

Replace *lrsDomain* with your Learning Locker domain, *lrsAuth* with the corresponding authentication, *mysqlUser* with the MySQL user name, *mysqlPassword* with the MySQL password, *mysqlHost* with the MySQL host, *mysqlPort* with the MySQL port, and *mysqlDatabase* with the MySQL database name. 

*Do not forget to persist you database data*

