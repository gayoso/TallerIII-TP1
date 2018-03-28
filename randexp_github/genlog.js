const randexp = require('randexp');
const fs = require('fs');

var numIter = process.argv[2];
var filename = "test_log_" + numIter + ".txt";

var logger = fs.createWriteStream(filename, {
  flags: 'a' // 'a' means appending (old data will be preserved)
});

var logs = [];
for (var i = 0; i < numIter; ++i) {
	
	var randomLog = "";
	if (logs.length <= 0 || Math.floor(Math.random() > 0.3)) {
		randomLog = new randexp("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3} [0-9]{2}/[0-9]{2}/[0-9]{4}:[0-9]{2}:[0-9]{2}:[0-9]{2} \\[(error|log)\\] \"(GET|POST|UPDATE|DELETE) /home(/[a-z]{1,9}){1,3} HTTP/1\\.1\" [0-9]{3} [a-z]{3,6} ([a-z]{3,6}){1,6}").gen();
		logs.push(randomLog);
	} else {
		randomLog = logs[Math.floor(Math.random()*logs.length)];
	}
	
	logger.write(randomLog);
	logger.write("\n");
}
logger.end();