open-replicator
===============

Open Replicator is a high performance MySQL binlog parser written in Java. It unfolds the possibilities that you can parse, filter and broadcast the binlog events in a real time manner.


### releases
1.3.0
    released: 2015-09-09
    support mysql 5.6 and binlog_checksums

1.2.1
    released: 2015-06-16
    notify io reader thread on exception from buffered stream reader, so it doesn't wait infinitely

1.2.0
    released: 2015-03-08
    preserve invalid datetime values (0000-00-00, anyone?) as longs inside the datetime


1.1.2
    released: 2015-02-20
    expose binlog filename in event stream
    rescue EOFException around doParse(); it's normal when the stream is closed

1.0.7

    release date: 2014-05-12
    support signed tinyint, smallint, mediumint, int, bigint

1.0.6

    release date: 2014-05-08
    remove dependency commons-lang, log4j
    support MYSQL_TYPE_TIMESTAMP2, MYSQL_TYPE_DATETIME2, MYSQL_TYPE_TIME2

1.0.0

    release date: 2011-12-29

### maven
```
<dependency>
        <groupId>com.zendesk</groupId>
        <artifactId>open-replicator</artifactId>
        <version>1.3.0</version>
</dependency>
```
### parsers

BinlogEventParser is plugable. All available implementations are registered by default, but you can register only the parsers you are interested in.
![Alt text](http://dl.iteye.com/upload/attachment/0070/3054/4274ab64-b6d2-380b-86b2-56afa0de523d.png)

### usage
```
final OpenReplicator or = new OpenReplicator();
or.setUser("root");
or.setPassword("123456");
or.setHost("localhost");
or.setPort(3306);
or.setServerId(6789);
or.setBinlogPosition(4);
or.setBinlogFileName("mysql_bin.000001");
or.setBinlogEventListener(new BinlogEventListener() {
    public void onEvents(BinlogEventV4 event) {
        // your code goes here
    }
});
or.start();

System.out.println("press 'q' to stop");
final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
for(String line = br.readLine(); line != null; line = br.readLine()) {
    if(line.equals("q")) {
        or.stop();
        break;
    }
}
```
