package application

import application.match.MatchView
import application.player.PlayerView
import application.tools.ToolsView
import javafx.geometry.Pos
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import session.Player
import session.Session
import tornadofx.*

class MainView : View() {
    override val root: Form = Form()
    private val playersGui: MutableList<PlayerView> = mutableListOf()
    private val matchesGui: MutableList<MatchView> = mutableListOf()
    private val session: Session by inject()
    private lateinit var utilsGui: ToolsView

    private fun cycleDatabase() {
        GlobalScope.launch {
            utilsGui.blinkDatabaseIndicator(session)

            delay(2048)
            cycleDatabase()
        }
    }

    private fun cycleMemScan() {
        GlobalScope.launch {
            utilsGui.blinkGuiltyGearIndicator(session)
            if (session.xrdApi.isConnected() && session.updatePlayers()) {
                redrawAppUi()
            }

            if (session.xrdApi.isConnected() && session.updateMatch()) {
                matchesGui[0].applyMatch(session.match)
            }

            delay(128)
            cycleMemScan()
        }
    }

    private fun cycleUi() {
        GlobalScope.launch {
            utilsGui.applyData(session)

            delay(64)
            cycleUi()
        }
    }

    private fun redrawAppUi() {
        utilsGui.blinkGearNetIndicator(session)

        // Sort and redraw PlayerViews
        val uiUpdate: List<Player> = session.players.values.toList()
            .sortedByDescending { item -> item.getRating() }
            .sortedByDescending { item -> item.getBounty() }
            .sortedByDescending { item -> if (!item.isIdle()) 1 else 0 }

        for (i in 0..7) {
            if (uiUpdate.size > i)
                playersGui[i].applyData(uiUpdate[i])
            else
                playersGui[i].applyData(Player())
        }
        // TODO: Sort and redraw MatchViews
    }

    init {
        with(root) {
            addClass(MainStyle.appContainer)
            translateY -= 5.0
            hbox {

                // ======== LEFT SIDE COLUMN ========
                vbox {
                    alignment = Pos.TOP_CENTER; spacing = 2.0
                    minWidth = 520.0
                    maxWidth = 520.0

                    // MATCH INFO
                    label("MATCH MONITORS") { addClass(MainStyle.lobbyName) }
                    // MATCH VIEWS
                    hbox { matchesGui.add(MatchView(parent)) }
                    hbox { matchesGui.add(MatchView(parent)) }
                    hbox { matchesGui.add(MatchView(parent)) }
                    hbox { matchesGui.add(MatchView(parent)) }
                }

                // ======== RIGHT SIDE COLUMN ========
                vbox {
                    alignment = Pos.TOP_CENTER; spacing = 2.0
                    minWidth = 420.0
                    maxWidth = 420.0

                    // LOBBY NAME
                    label("LOBBY_TITLE_FULL") { addClass(MainStyle.lobbyName) }
                    // PLAYER VIEWS
                    for (i in 0..7) hbox { playersGui.add(PlayerView(parent)) }
                }

            }

            // ======== BOTTOM UTILS ========
            hbox { utilsGui = ToolsView(parent) }

            cycleDatabase()
            cycleMemScan()
            cycleUi()
        }
    }
}



