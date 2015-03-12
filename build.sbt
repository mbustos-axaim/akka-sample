name := "salary-history"

version := "1.0"

scalaVersion := "2.11.4"

mainClass in Compile := Some("com.realityball.salaryhistory.RotoGuruDailyHistoryLoad")

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "org.slf4j"           % "slf4j-simple" % "1.6.4",
  "com.h2database"      % "h2" % "1.3.175",
  "org.scalatest"      %% "scalatest" % "2.1.6" % "test",
  "io.spray"           %%  "spray-json"    % "1.3.1",
  "mysql"               % "mysql-connector-java" % "latest.release",
  "org.seleniumhq.webdriver" % "webdriver-selenium" % "0.9.7376",
  "org.seleniumhq.webdriver" % "webdriver-htmlunit" % "0.9.7376"
)
