package com.realityball.salaryhistory

import RealityballConfig._
import RotoGuruRecords._
import org.apache.html.dom
import com.thoughtworks.selenium
import org.openqa.selenium.By
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.slick.lifted.AbstractTable
import scala.util.matching.Regex
import scala.io.Source
import scala.collection.parallel._
import java.io._
import org.openqa.selenium.JavascriptExecutor


/**
 * Created by pedro on 1/6/15.
 */
object RotoGuruDailyHistoryLoad extends App {
  
  import scala.collection.mutable.MutableList
  
  val logger =  LoggerFactory.getLogger(getClass)

  val rotoGuruLinkExpression = """[^a-z0-9]""".r
  val htmlCleaningExpression = """<tr><tr>[\s\S]*name\)<\/font><\/td><\/tr>""".r
  val playerDetailExpression: Regex = """Name: <FONT size=\+1><B>(.*), (.*)<\/B><\/FONT><BR><\/BR><FONT size=\+0>Team: <B>(.*)zzz<\/B><BR><\/BR>(.*): <B>(.*)<\/B><BR></BR>Links.*playerId=([0-9]*).*player_id=([0-9]*).*""".r
  val playerDetailExpression0: Regex = """Name: <FONT size=\+1><B>(.*), (.*)<\/B><\/FONT><BR><\/BR><FONT size=\+0>Team: <B>(.*)zzz<\/B><BR><\/BR>.*""".r
  val gameIdExpression = """.*gid=[0-9]*_[0-9]*_[0-9]*_(.*)mlb_(.*)mlb_([0-9])&.*""".r
  
  // find players already loaded
  val loadedPlayers = db.withSession{ implicit session =>
    rotoGuruPlayersTable.map(_.rotoGuruId).run.toList
  }

  val tables: Map[String, TableQuery[_ <: slick.driver.MySQLDriver.Table[_]]] = Map (
      ("rotoGuruPlayers" -> rotoGuruPlayersTable), ("rotoGuruDailySalary" -> rotoGuruDailySalaryTable), ("rotoGuruDailyResults" -> rotoGuruDailyResultsTable))

  def maintainDatabase = {
    db.withSession { implicit session =>
      def maintainTable(schema: TableQuery[_ <: slick.driver.MySQLDriver.Table[_]], name: String) = {
        if (!MTable.getTables(name).list.isEmpty) {
          schema.ddl.drop
        }
        schema.ddl.create
      }
      tables.map {case (k, v) => maintainTable(v, k)}
    }
  }  
  
  def cleanSource(link: String) : String = {

    val fileName = "/tmp/" + rotoGuruLinkExpression.replaceAllIn(link, "_")
    val file = new File(fileName)
    if (!file.exists()) {  // if file hasn't already been saved off, download it and clean it
      val driver = new HtmlUnitDriver  //(true)
      driver.get(link)
    
      val pageTxt = driver.getPageSource()
      val newSource = htmlCleaningExpression.replaceAllIn(pageTxt, "")

      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(newSource)
      bw.close()
      logger.info("  cleanSource wrote: " + fileName)
    }
  
    return(fileName)
  }

  def substringAfter(s:String, k:String) = {
    s.indexOf(k) match {
      case -1 => ""
      case i => s.substring(i+k.length)
      }
  }
    
  def processRow(x: WebElement, playerId: String) = {
    /*<tr >
0   <td bgcolor=FFCC99 align=center>09-06</td>
1   <td bgcolor=FFCC99 align=center nowrap>
    <a href=http://mlb.mlb.com/mlb/gameday/index.jsp?gid=2014_09_06_seamlb_texmlb_1&mode=box target=_blank>Sea</a></td>
2   <td bgcolor=FFCC99 align=center>^<sup>2</sup>SS</td>
3   <td bgcolor=FFCC99 align=center><a title="R-Young, Chris">R</a></td>
4   <td bgcolor=CCFF99 align=center><a title=" 0/5 2SO ">-1.5</a></td>
5   <td bgcolor=CCFF99 align=right> </td>
<!---<td bgcolor=CCFF99 align=right> </td>---->
6   <td bgcolor=CCFFFF align=center><a title=" 0/5 2SO ">-1.25</a></td>
7   <td bgcolor=CCFFFF align=right>$2,400 </td>
8   <td bgcolor=99FF00 align=center><a title=" 0/5 2SO ">     -5.00</a></td>
9   <td bgcolor=99FF00 align=right> </td>
10 <td bgcolor=FFFFCC align=center><a title=" 0/5 2SO ">     -0.50</a></td>
11 <td bgcolor=FFFFCC align=right> </td>
12 <td bgcolor=F9C3C3 align=center><a title=" 0/5 2SO ">-5</a></td>
13 <td bgcolor=F9C3C3 align=right>$7,550 </td>
14 <td bgcolor=FFFF99 align=center><a title=" 0/5 2SO ">  0.00</a></td>
15 <td bgcolor=FFFF99 align=right>$3,600 </td>
</tr>*/

    val tableCells = x.findElements(By.xpath("td"))
    if(!tableCells.isEmpty()) {
      var salaryRecords = List.empty[RotoGuruDailySalary]
      try {
        val fanDuelPriceStr = tableCells(7).getText().replaceAll("""\$""","").replaceAll(",","")
        val fanDuelPrice = if(fanDuelPriceStr.equals("")) { 0 } else { fanDuelPriceStr.toInt }
        val fanThrowdownPriceStr = tableCells(11).getText().replaceAll("""\$""","").replaceAll(",","")
        val fanThrowdownPrice = if(fanThrowdownPriceStr.equals("")) { 0 } else { fanThrowdownPriceStr.toInt }
        val draftDayPriceStr = tableCells(13).getText().replaceAll("""\$""","").replaceAll(",","")
        val draftDayPrice = if(draftDayPriceStr.equals("")) { 0 } else { draftDayPriceStr.toInt }
        val draftKingsPriceStr = tableCells(15).getText().replaceAll("""\$""","").replaceAll(",","")
        val draftKingsPrice = if(draftKingsPriceStr.equals("")) { 0 } else { draftKingsPriceStr.toInt }
        if(fanDuelPrice>0 || fanThrowdownPrice>0 || draftDayPrice>0 || draftKingsPrice>0) {
          val tdDate = tableCells(0).getText()
          val gameDate = "2014" + "/" + tdDate.substring(0,2) + "/" + tdDate.substring(3,5)
/*         val mlbLinkRef = tableCells(1).findElement(By.xpath("a"))
          val mlbLink = mlbLinkRef.getAttribute("href").toString()
          val positionString = tableCells(2).getText()
          val pitcherString = tableCells(3).getText()
          val fanDuelPointsString = tableCells(6).getText()
          val fanThrowdownPointsString = tableCells(10).getText()
          val draftDayPointsString = tableCells(12).getText()
          val draftKingsPointsString = tableCells(14).getText()
*/        
          salaryRecords = RotoGuruDailySalary(playerId, gameDate, fanDuelPrice, fanThrowdownPrice, draftDayPrice, draftKingsPrice) :: salaryRecords
          
/*          val isDoubleheader = positionString.contains("|")
          mlbLink match { 
            case gameIdExpression(homeTeam,awayTeam,gamenum) => {
              val gameId = homeTeam.toUpperCase() + gameDate.replaceAll("/","")
              if(tableCells(1).getText().equals("pp")) {
                //rotoGuruDailyHistoryTable += RotoGuruDailyHistory("")
              } else {
                
              }
            }
          }
*/
        }
        db.withTransaction { implicit session => {
          rotoGuruDailySalaryTable ++= salaryRecords.toIterable
        } }
      } catch {
        case ex: Exception => {
          logger.info("Exception thrown processing row!  " + ex.getMessage + "\n  ... Row Contents: " + x.getValue)
        }
      }
    }
  }
  
  def processOption(option: WebElement) = {
    val playerId = option.getAttribute("value")
    logger.info("playerId: " + playerId)
    if(playerId != "0000" & playerId != "7476" & playerId != "107y" & playerId != "1886" & playerId != "122x" & !loadedPlayers.contains(playerId)) {
      val optionText = option.getText()
      val commaIndex = optionText.indexOf(",")
      val firstParenIndex = optionText.indexOf("(")
      val lastName = optionText.substring(0, commaIndex)
      val firstName = optionText.substring(commaIndex+2, firstParenIndex-1)
      val team = optionText.substring(firstParenIndex+1, firstParenIndex+4).toUpperCase()
      val initialUrl = "http://rotoguru1.com/cgi-bin/player.cgi?" + playerId + "x"
      println("Processing: " + lastName + "," + firstName + " (" + team + ") / id: " + playerId + ", url: " + initialUrl)
      val driverFile = "file://Localhost" + cleanSource(initialUrl)
      val driver = new HtmlUnitDriver
      driver.get(driverFile)
      val playerXpath = driver.findElement(
          By.xpath("/html/body/center[2]/font/font/font/table/tbody/tr[1]/td[1]/table/tbody/tr/td")
          )
      val driver2 = new HtmlUnitDriver(true)  // Javascript enabled
      val js: JavascriptExecutor = driver2.asInstanceOf[JavascriptExecutor]
      ////js.executeScript("return arguments[0].innerHTML;", playerXpath).toString
      val tdData = js.executeScript("return arguments[0].innerHTML;", playerXpath).toString.replaceAll("\n", "zzz") //"<FONT size=+1><B>"

    //val simpleExp = """.*(FONT size=.[0-9]).*""".r

    //println(bob)

    //bob match { case simpleExp(fontSize) => { println(fontSize) } }
      /*
      val bob = "<FONT size=+1><B>" 
      val simpleExp = """.?(FONT size=.[0-9]).""".r
      println(bob)
      bob match { case simpleExp(fontSize) => {println(fontSize)} }
      */ 
      //val playerDetailExpression(lastname1, firstname1, team1, batsHits, hand, espnId, mlbId) = bob
      //val joe() = playerDetailExpression.findAllIn(bob).toList
      //joe.map(println)
      db.withTransaction { implicit session =>
        tdData match {
          //case playerDetailExpression0(lastName1, firstName1, team1) => { println(lastName1) }
          case playerDetailExpression(lastName1, firstName1, team1, batsHits, hand, espnId, mlbId) => {
            if(batsHits == "Bats") {
              rotoGuruPlayersTable += RotoGuruPlayer(playerId, "2014", lastName1, firstName1, hand, "NA", team1, "hitter", mlbId, espnId)  
            } else {
              rotoGuruPlayersTable += RotoGuruPlayer(playerId, "2014", lastName1, firstName1, "NA", hand, team1, "pitcher", mlbId, espnId)  
            }
          }
        }
      }      
      val mainTable = driver.findElement(
          By.xpath("/html/body/center[2]/font/font/font/table/tbody/tr[2]/td/p[1]/table/tbody/tr/td/table/tbody") 
          )
      val rows = mainTable.findElements(By.xpath("tr[not(@*)]"))
      logger.info("  ... processing rows ...")
      rows.map( processRow(_, playerId) )
    }
  }

  //maintainDatabase
  
  // first get full list of available players from HTML already downloaded by hand (to clean up invalid HTML)
  val driver = new HtmlUnitDriver  //  (true)  //to activate Javascript
  driver.get("file://Localhost/Users/pedro/Documents/keeplearning/MLB/baseballplayerlookup2.html")
  
  val pitcherOpts1 = driver.findElements(By.xpath("/html/body/center[2]/font/font/font[1]/p/table/tbody/tr/td/table/tbody/tr[2]/td[1]/form/select/option"))
  val pitcherOpts2 = driver.findElements(By.xpath("/html/body/center[2]/font/font/font[1]/p/table/tbody/tr/td/table/tbody/tr[2]/td[2]/form/select/option"))  
  val pitcherOpts = List.concat(pitcherOpts1, pitcherOpts2)
  val hitterOpts1 = driver.findElements(By.xpath("/html/body/center[2]/font/font/font[1]/p/table/tbody/tr/td/table/tbody/tr[3]/td[1]/form/select/option"))
  val hitterOpts2 = driver.findElements(By.xpath("/html/body/center[2]/font/font/font[1]/p/table/tbody/tr/td/table/tbody/tr[3]/td[2]/form/select/option"))  
  val hitterOpts = List.concat(hitterOpts1,hitterOpts2)
  val allOpts = List.concat(hitterOpts, pitcherOpts)

  //val fileLines = io.Source.fromFile("/Users/pedro/Documents/keeplearning/MLB/salaryscrape2.html").getLines.toList
  //fileLines.foreach(println)
  
  allOpts.map( {
    processOption  
  } )

  /*
  val playerId = "5125"
  val initialUrl = "http://rotoguru1.com/cgi-bin/player.cgi?" + playerId + "x"  // get Cabrera's data to start
  
  val cleanFileName = cleanSource("http://rotoguru1.com/cgi-bin/player.cgi")
  driver.get("file://Localhost" + cleanFileName)
  val cleanFileName2 = cleanSource(initialUrl)
  driver.get("file://Localhost" + cleanFileName2)
  
  val pageTxt = driver.getPageSource()
  val regx = """<tr><tr>[\s\S]*name\)<\/font><\/td><\/tr>""".r
  val newSource = regx.replaceAllIn(pageTxt, "")
  */
  
  //txt.r
  //println("I'm here")
  //val link = driver.findElement(
  //  By.xpath("//a[@href='http://mlb.mlb.com/mlb/gameday/index.jsp?gid=2014_04_03_mlb_detmlb_1&mode=box target=_blank']/parent"))
  //val link = driver.findElement(
  //    By.xpath("//a[contains(@href, 'http://mlb.mlb.com/mlb/gameday/index.jsp?gid=2014_04_03')]")
  //    )
  
  // tr[not(@*)] means select rows that don't have an attribute set (http://www.zvon.org/comp/r/tut-XPath_1.html#Pages~Attributes)
        //By.xpath("//html/body/center[2]/font/font/font/table/tbody/tr[2]/td/p/table/tbody/tr/td/table/tbody/tr[not(@*)]")
  ///html/body/center[2]/font/font/font/table/tbody/tr[2]/td[@colspan='2']
  /*
  val mainTable = driver.findElement(
      By.xpath("//html/body/center[2]/font/font/font/table/tbody/tr[2]/td/p[1]/table/tbody/tr/td/table/tbody") //[2]/td[@colspan='2'][1]")
  )
  */
//val element = mainTable.findElement(By.xpath("tr[1]"))
//driver.executeScript("""
//var tbl = arguments[0];
//var trs = tbl.getElementsByTagName('tr');
//tbl.removeChild(trs[7]);
//tbl.removeChild(trs[5]);
//tbl.removeChild(trs[4]);
//tbl.removeChild(trs[3]);
//tbl.removeChild(trs[2]);
//tbl.removeChild(trs[1]);
//tbl.removeChild(trs[0]);
//""", mainTable)
  //driver.executeScript("document", mainTable)
      //html/body/center[2]/font/font/font/table/tbody/tr[2]/td/p[1]/table/tbody/tr/td/table/tbody/tr[6]
  //val rows = mainTable.findElements(By.xpath("a/ancestor::tr[1]"))
/*
val rows = mainTable.findElements(By.xpath("tr[not(@*)]"))
*/
//td[1]/p[1]/table/tbody/tr/td/table/tbody/tr[not(@*)]"))

      //println(link.getTagName)
  //println(link.getText)
  //  val row = "<tr >
  //                <td bgcolor=FFCC99 align=center>10-05</td>
  //              <td bgcolor=FFCC99 align=center nowrap><a href=htt=... target=_blank >Bal</a></td>
  //              <td bgcolor=FFCC99 align=center>^<sup>3</sup>1B</td>
  //              <td bgcolor=FFCC99 align=center><a title="R-Norris, Bud">R</a></td>
  //              <td bgcolor=CCFF99 align=center><a title=" 0/4 SO ">-0.75</a></td>
  //              <td bgcolor=CCFF99 align=right> </td>
//<!---<td bgcolor=CCFF99 align=right> </td>---->
  //              <td bgcolor=CCFFFF align=center><a title=" 0/4 SO ">-1</a></td>
  //              <td bgcolor=CCFFFF align=right>$5,100 </td>
  //              <td bgcolor=99FF00 align=center><a title=" 0/4 SO ">     -4.00</a></td>
  //              <td bgcolor=99FF00 align=right> </td>
  //              <td bgcolor=FFFFCC align=center><a title=" 0/4 SO ">     -0.25</a></td>
  //              <td bgcolor=FFFFCC align=right> </td>
  //              <td bgcolor=F9C3C3 align=center><a title=" 0/4 SO ">-4</a></td>
  //              <td bgcolor=F9C3C3 align=right>$11,600 </td>
  //              <td bgcolor=FFFF99 align=center><a title=" 0/4 SO ">  0.00</a></td>
  //              <td bgcolor=FFFF99 align=right>$5,600 </td></tr>
  
  /*    
  def processRow(x: WebElement) = {
    val tableCells = x.findElements(By.xpath("//td"))
    if(!tableCells.isEmpty()) {
      println(tableCells(0).getText)
    }
  }
  
  rows.map( {
    println
  })
  rows.map { processRow }
  */
    
  //val link2 = link.findElement(By.xpath)
  //val link = driver.findElementByLinkText("http://mlb.mlb.com/mlb/gameday/index.jsp?gid=")
  //println(link.getElementName)
  //println(link.getText)

  //val peers = driver.findElementsByXPath("//*[@id=\"table_peers4\"]/tbody/tr/td[position() = 1 or position() = 2]")

  // zip up the list in pairs so List(a,b,c,d) becomes List((a,b), (c,d))
  //for(peer <- peers zip peers.tail) {
  //println(peer)
  //}

}
