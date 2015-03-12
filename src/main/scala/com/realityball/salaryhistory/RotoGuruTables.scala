package com.realityball.salaryhistory

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.lifted.{ProvenShape, ForeignKeyQuery}
import spray.json._
import DefaultJsonProtocol._

object RotoGuruRecords {
  import scala.collection.mutable.Queue

  case class RotoGuruPlayer(rotoGuruId: String, year: String, lastName: String, firstName: String,
      batsWith: String, throwsWith: String, team: String, position: String, mlbComId: String, 
      espnId: String)
      
  case class RotoGuruDailySalary(rotoGuruId: String, date: String,
      fanDuelSalary: Int, fanThrowdownSalary: Int, draftDaySalary: Int, draftKingsSalary: Int)
      
  case class RotoGuruDailyResults(rotoGuruId: String, game: String, date: String, opposing: String, position: String, lineupPosition: Int,
      opposingPitcher: String, opposingPitcherHandedness: String, fanDuelPoints: Double, fanDuelEvents: String,
      fanThrowdownPoints: Double, fanThrowdownEvents: String,
      draftDayPoints: Double, draftDayEvents: String,
      draftKingsPoints: Double, draftKingsEvents: String, note: String)      

  val rotoGuruPlayersTable: TableQuery[RotoGuruPlayersTable] = TableQuery[RotoGuruPlayersTable]
  val rotoGuruDailySalaryTable: TableQuery[RotoGuruDailySalaryTable] = TableQuery[RotoGuruDailySalaryTable]
  val rotoGuruDailyResultsTable: TableQuery[RotoGuruDailyResultsTable] = TableQuery[RotoGuruDailyResultsTable]
}

import RotoGuruRecords._

class RotoGuruPlayersTable(tag: Tag) extends Table[RotoGuruPlayer](tag, "rotoGuruPlayers") {
  def rotoGuruId = column[String]("rotoGuruId", O.PrimaryKey);
  def year = column[String]("year");
  def lastName = column[String]("lastName")
  def firstName = column[String]("firstName")
  def batsWith = column[String]("batsWith")
  def throwsWith = column[String]("throwsWith")
  def team = column[String]("team")
  def position = column[String]("position")
  def mlbComId = column[String]("mlbComId")
  def espnId = column[String]("espnId")

  // def pk = primaryKey("pk_id_date", (rotoGuruId, year))

  def * = (rotoGuruId, year, lastName, firstName, batsWith, throwsWith, team, position,
      mlbComId, espnId) <> (RotoGuruPlayer.tupled, RotoGuruPlayer.unapply)
}

class RotoGuruDailySalaryTable(tag: Tag) extends Table[RotoGuruDailySalary](tag, "rotoGuruDailySalary") {
  
  def rotoGuruId = column[String]("rotoGuruId")
  def date = column[String]("date")
  def fanDuelSalary = column[Int]("fanDuelSalary")
  def fanThrowdownSalary = column[Int]("fanThrowdownSalary")
  def draftDaySalary = column[Int]("draftDaySalary")
  def draftKingsSalary = column[Int]("draftKingsSalary")

  def pk = primaryKey("pk_id_date", (rotoGuruId, date))

  def * = (rotoGuruId, date, fanDuelSalary, fanThrowdownSalary, draftDaySalary, draftKingsSalary) <> 
    (RotoGuruDailySalary.tupled, RotoGuruDailySalary.unapply)
}

class RotoGuruDailyResultsTable(tag: Tag) extends Table[RotoGuruDailyResults](tag, "rotoGuruDailyResults") {
  
  def rotoGuruId = column[String]("rotoGuruId")
  def game = column[String]("game")
  def date = column[String]("date")
  def opposing = column[String]("opposing")
  def position = column[String]("position")
  def lineupPosition = column[Int]("lineupPosition")
  def opposingPitcher = column[String]("opposingPitcher")
  def opposingPitcherHandedness = column[String]("opposingPitcherHandedness")
//  def draftStreetPoints = column[Double]("draftStreetPoints")  // acquired by DraftKings
//  def draftStreetEvents = column[String]("draftStreetEvents")
//  def draftStreetSalary = column[Int]("draftStreetSalary")
  def fanDuelPoints = column[Double]("fanDuelPoints")
  def fanDuelEvents = column[String]("fanDuelEvents")
//  def starStreetPoints = column[Double]("starStreetPoints")  // acquired by DraftKings
//  def starStreetEvents = column[String]("starStreetEvents")
//  def starStreetSalary = column[Int]("starStreetSalary")
  def fanThrowdownPoints = column[Double]("fanThrowdownPoints")
  def fanThrowdownEvents = column[String]("fanThrowdownEvents")
  def draftDayPoints = column[Double]("draftDayPoints")
  def draftDayEvents = column[String]("draftDayEvents")
  def draftKingsPoints = column[Double]("draftKingsPoints")
  def draftKingsEvents = column[String]("draftKingsEvents")
  def note = column[String]("note")

  def pk = primaryKey("pk_id_game", (rotoGuruId, game))

  def * = (rotoGuruId, game, date, opposing, position, lineupPosition, opposingPitcher, opposingPitcherHandedness, 
      fanDuelPoints, fanDuelEvents, fanThrowdownPoints, fanThrowdownEvents, draftDayPoints, draftDayEvents, 
      draftKingsPoints, draftKingsEvents, note) <> (RotoGuruDailyResults.tupled, RotoGuruDailyResults.unapply)

}