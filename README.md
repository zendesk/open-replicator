open-replicator
===============

Open Replicator is a high performance MySQL binlog parser written in Java. It unfolds the possibilities that you can parse, filter and broadcast the binlog events in a real time manner.


### releases

1.5.0

    release: 2016-09-07
    support a "stop-on-EOF" mode that borrows what mysqldump does, exiting when the server has no more
    binlog events.

1.4.4

    release: 2016-09-01
    fixes incorrect nanosecond calcuation when using TIMESTAMP(X), DATETIME(X) and TIME(X) columns in 5.6

1.4.3

    release: 2016-06-05
    setup @slave_uid session variable for 5.6-compatible slave-uniqueness checks

1.4.2

    release: 2016-03-01
    Implment server-sent heartbeats and an API to check up on them.

1.4.1

    release: 2016-01-29
    pickup a couple of upstream fixes -- Gtid event parsing and something around closing the stream

1.4.0

    release: 2016-01-06
    support GIS extensions via vividsolution's 'jts' library

1.3.6

    release: 2015-12-14
    bug-fix: Fix issue with binlog_row_image = MINIMAL parsing overruns

1.3.5

    release: 2015-11-04
    bug-fix: don't emit format description events unless asked for in file replication
    bug-fix: support checksums even if the consumer didn't ask for format description events

1.3.4

    release: 2015-11-03
    bug-fix: support mysql 5.6 file-based replication when the offset is greater than 4

1.3.3

    release: 2015-10-30
    support 5.6 checksums in file-based replication

1.3.2

    release: 2015-10-07
    bugfix for a big-endian issue that was corrupting BIT() columns longer than 2 bytes

1.3.1

    release: 2015-09-28
    provide a helpful error message when an old authentication scheme is detected

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
