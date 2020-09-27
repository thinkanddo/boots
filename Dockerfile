FROM harbor.cop.com:8443/library/tomcat:8.5.38
  
COPY target/boots.jar /opt

CMD java -jar /opt/boots.jar --environment=false --work-dir-ansible=/bcos/bcos-admin --server.port=8080 --logging.level.root=info --logging.path=/var/log/boots --spring.datasource.type=com.zaxxer.hikari.HikariDataSource --spring.datasource.username=${MYSQL_USERNAME} --spring.datasource.password=${MYSQL_PASSWORD} --spring.datasource.url=jdbc:mysql://${MYSQL_IP}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=UTF-8&useSSL=true 
