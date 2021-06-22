package database

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate

//Soon to be Deprecated in favor of Web API
class DatabaseHandler : SqlApi {
    private val connector: Jdbi = Jdbi.create("jdbc:sqlite:arcNet").installPlugins()
    private val daoClass = SqlApiDao::class.java

    // DatabaseHandler's init first executes on line 14 in Session.kt
    init {
        // create the tables if they don't already exist
        // can this be made nicer? without having to hardcode the tables
        connector.open().use {
            // create the fightData table if it doesn't exist
            it.execute("create table if not exists fightdata (winnerid bigint, winnerchar smallint, fallenid bigint, fallenchar smallint, occurences int, unique(winnerid, winnerchar, fallenid, fallenchar))")
            //create the userData table if it doesn't exist
            it.execute("create table if not exists userdata(id bigint unique, displayname text, matcheswon int, matchessum int, bountywon int, bountysum int)")
        }
    }

    // this class does not have any persistent connection, and would be wasteful to possible create one each time
    // this method is called. besides if we cannot connect, then the init block will fail!
    override fun isConnected(): Boolean = true

//    override fun isConnected(): Boolean {
//        return try {
//            connector.open().use { true }
//        } catch(e: ConnectionException) {
//            false
//        }
//    }

    override fun getLegacyData(steamId: Long): LegacyData = useDao { it.getData(steamId) }

    override fun putLegacyData(legacy: LegacyData) = useDao { it.putData(legacy) }

    override fun getFightData(): List<FightData> = useDao { it.getFightData() }

    override fun putFightData(fight: FightData) = useDao { it.putFightData(fight)}

    private fun <T> useDao(callback: (dao: SqlApiDao) -> T): T =
            connector.open().use { callback.invoke(it.attach(daoClass)) }
}

interface SqlApiDao {
    @SqlQuery("select * from userData where steamId = :steamId")
    fun getData(steamId: Long): LegacyData

    @SqlUpdate(
        "insert into" +
                "  userData(steamId, displayName, matchesWon, matchesSum, bountyWon, bountySum)\n" +
                "  values (:legacy.steamId, :legacy.displayName, :legacy.matchesWon, :legacy.matchesSum, :legacy.bountyWon, :legacy.bountySum)\n" +
                "    on conflict (steamId)" +
                "       do update set" +
                "         displayName = :legacy.displayName," +
                "             matchesWon = :legacy.matchesWon," +
                "             matchesSum = :legacy.matchesSum," +
                "             bountyWon = :legacy.bountyWon," +
                "             bountySum = :legacy.bountySum" +
                "             where userData.steamId = :legacy.steamId"
    )
    fun putData(legacy: LegacyData)

    @SqlQuery("select * from fightData")
    fun getFightData(): List<FightData>

    @SqlUpdate(
            "insert into\n" +
                    "  fightData(winnerId, fallenId, winnerChar, fallenChar, occurrences)\n" +
                    "  values(:fight.winnerId, :fight.fallenId, :fight.winnerChar, :fight.fallenChar, 1)\n" +
                    "  on conflict(winnerId, fallenId, winnerChar, fallenChar)\n" +
                    "     do update set occurrences = fightData.occurrences + 1;"
    )
    fun putFightData(fight: FightData)
}
