# Mysql Replication Checksums


## For a live replica:

- First, the replicator will check the global variable `@@global.binlog_checksum`.  If this variable's value is 'CRC32',
  we will set the session variable `@master_binlog_checksum` to 'CRC32' as well.  This indicates to the server that
  the replicator is capable of receiving checksums.

- Now the master will send a format_log_description event.  The replicator will check the server version of the event.
  If the server version is >= 5.6.1, the event will be 6 bytes longer than normal -- two bytes indicating checksum status,
  and a 4 byte checksum.

- If checksumming is enable on the master, each event will now be 4 bytes longer.
