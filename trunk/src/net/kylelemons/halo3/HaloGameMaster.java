package net.kylelemons.halo3;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import net.kylelemons.halo3.GameList.GameType;
import net.kylelemons.halo3.GameSetup.SetupChangeListener;
import net.kylelemons.halo3.MapList.Map;
import net.kylelemons.halo3.PlayerList.Player;
import net.kylelemons.halo3.RemoteUpdateClient.RemoteUpdateListener;

public class HaloGameMaster implements GameGenerator.GameChangedListener, UserInterface.KeyListener,
    SetupChangeListener, RemoteUpdateListener
{
  private UserInterface      m_ui;
  private GameGenerator      m_gen;
  private PlayerList         m_players;
  private GameList           m_games;
  private MapList            m_maps;

  private GameSetup          m_setup;

  // TODO Logging
  public static final String APPLICATION = "HaloGameMaster";
  public static final String VERSION     = "v0.1.8";
  private long               m_regen_start_time;
  private SimpleWebInterface m_webserver;
  private Thread             m_webthread;
  private RemoteUpdateServer m_remoteupdate;
  private Thread             m_remotethread;
  private Thread             m_clientthread;
  private RemoteUpdateClient m_remoteclient;
  private boolean            m_clientstarted;
  private boolean            m_serverstarted;
  private boolean            m_paused;

  private static Logger      logger      = Logger.getLogger("net.kylelemons.halo3");

  /**
   * @param args
   * @throws InterruptedException
   * @throws IOException
   * @throws SecurityException
   */
  public static void main(String[] args) throws InterruptedException, SecurityException, IOException
  {

    HaloGameMaster master = new HaloGameMaster();
    master.start();
  }

  public HaloGameMaster() throws SecurityException, IOException
  {
    // Setup logging
    // Send logging output to file
    FileHandler fh = new FileHandler("Halo3.log"); // "%t/Halo3.log"
    fh.setFormatter(new SimpleFormatter());
    logger.addHandler(fh); // fh.setFormatter(new SimpleFormatter());

    // Request that every detail gets logged.
    logger.setLevel(Level.ALL);

    // Log a simple INFO message.
    logger.info(APPLICATION + "-" + VERSION + " Starting...");

    logger.fine("Creating Generator Setup");
    m_setup = new GameSetup();

    logger.fine("Creating Game Generator");
    m_gen = new GameGenerator(m_setup);

    logger.fine("Creating User Interface");
    m_ui = new UserInterface();
    m_ui.setSetup(m_setup);

    logger.fine("Creating Default Configuration");
    // These are the default players if none are loaded from the database
    m_players = new PlayerList();
    m_players.add(new PlayerList.Player("Example", "ex", 2));
    m_players.add(new PlayerList.Player("Players", "Zap", 3));
    m_players.add(new PlayerList.Player("Add Your Own", "Eko", 5));
    m_players.add(new PlayerList.Player("Press F1", "F1", 1, 50, false));
    m_players.add(new PlayerList.Player("Press F2", "F2", 2));
    m_players.add(new PlayerList.Player("Press F3", "F3", 3));
    m_players.add(new PlayerList.Player("Press F4", "F4", 4));
    m_players.add(new PlayerList.Player("Hold F5", "F5", 5));
    m_players.add(new PlayerList.Player("Quit is \"q\"", "quit", 0));
    m_players.sort(PlayerList.Player.SORT_BY_NAME);
    m_ui.setPlayerList(m_players);
    m_gen.setPlayerList(m_players);

    m_games = new GameList();
    m_games.add(new GameList.GameType("Team Slayer", 5));
    m_games.add(new GameList.GameType("Team Oddball", 3));
    m_games.add(new GameList.GameType("Team CTF", 2, false));
    m_games.add(new GameList.GameType("Team King", 2));
    m_games.add(new GameList.GameType("VIP", 1));
    m_games.add(new GameList.GameType("Zombies", 1, false));
    m_games.add(new GameList.GameType("Rocket Swords", 1, false));
    m_games.add(new GameList.GameType("Shotty Sniper", 1));
    m_games.add(new GameList.GameType("Ninja", 1, false));
    m_games.sort();
    m_ui.setGameList(m_games);
    m_gen.setGameList(m_games);

    m_maps = new MapList();
    m_maps.add(new MapList.Map("Blackout", "images/Blackout.jpg", 4, 10, 3));
    m_maps.add(new MapList.Map("Cold Storage", "images/Cold_Storage.jpg", 2, 6, 3));
    m_maps.add(new MapList.Map("Foundry", "images/Foundry.jpg", 4, 10, 3));
    m_maps.add(new MapList.Map("Ghost Town", "images/Ghost_Town.jpg", 4, 12, 3));
    m_maps.add(new MapList.Map("Guardian", "images/Guardian.jpg", 2, 8, 3));
    m_maps.add(new MapList.Map("Epitaph", "images/Epitaph.jpg", 4, 10, 2));
    m_maps.add(new MapList.Map("Narrows", "images/Narrows.jpg", 4, 10, 2));
    m_maps.add(new MapList.Map("Avalanche", "images/Avalanche.jpg", 10, 16, 1));
    m_maps.add(new MapList.Map("Construct", "images/Construct.jpg", 8, 12, 1));
    m_maps.add(new MapList.Map("High Ground", "images/High_Ground.jpg", 6, 12, 1));
    m_maps.add(new MapList.Map("Isolation", "images/Isolation.jpg", 8, 10, 1));
    m_maps.add(new MapList.Map("Last Resort", "images/Last_Resort.jpg", 4, 16, 1));
    m_maps.add(new MapList.Map("Rat's Nest", "images/Rat's_Nest.jpg", 6, 16, 1));
    m_maps.add(new MapList.Map("Sandtrap", "images/Sandtrap.jpg", 10, 16, 1));
    m_maps.add(new MapList.Map("Snowbound", "images/Snowbound.jpg", 12, 16, 1));
    m_maps.add(new MapList.Map("Valhalla", "images/Valhalla.jpg", 10, 16, 1));

    m_maps.add(new MapList.Map("Assembly", "images/Assembly.jpg", 2, 8, 3));
    m_maps.add(new MapList.Map("Orbital", "images/Orbital.jpg", 4, 12, 3));
    m_maps.add(new MapList.Map("The Pit", "images/The Pit.jpg", 4, 10, 3));
    m_maps.add(new MapList.Map("Sandbox", "images/Sandbox.jpg", 4, 12, 3));
    m_maps.add(new MapList.Map("Standoff", "images/Standoff.jpg", 4, 12, 3));
    m_maps.sort();
    m_ui.setMapList(m_maps);
    m_gen.setMapList(m_maps);

    logger.fine("Running UI Setup");
    m_ui.setup(); // the last thing this does is load the database, overwriting
    // the above

    m_setup.addSetupChangeListener(this);

    logger.info("Setting up web server");
    m_webserver = new SimpleWebInterface();

    logger.info("Setting up remote update server");
    m_remoteupdate = new RemoteUpdateServer();
    m_serverstarted = false;

    logger.info("Setting up remote update client");
    m_remoteclient = new RemoteUpdateClient();
    m_clientstarted = false;

    logger.info("Setup Complete");
  }

  public void start()
  {
    logger.info("Starting " + APPLICATION);
    m_gen.addGameChangedListener(this);
    m_ui.addKeyListener(this);
    m_gen.generate();

    logger.info("Starting webserver");
    m_webthread = new Thread(m_webserver);
    m_webthread.start();

    m_remotethread = new Thread(m_remoteupdate);

    m_clientthread = new Thread(m_remoteclient);
  }

  public void gameChanged(GameGenerator gen)
  {
    Team[] teams = gen.getTeams();
    GameType game = gen.getGame();
    Map map = gen.getMap();
    boolean good = gen.getGood();

    if (!m_clientstarted)
    {
      logger.info("Updating game UI");
      m_paused = false;
      m_ui.setTimerPaused(m_paused);
      if (m_serverstarted) m_remoteupdate.setTimerPaused(m_paused);
      m_ui.setGame(teams, game, map);
      m_webserver.setGame(teams, game, map);
      m_remoteupdate.setGame(teams, game, map, m_setup.getGameDelay());
      m_ui.setWarningBorder(good ? Color.BLACK : Color.RED);
    }
    else
      logger.info("Ignoring local update -- in client mode");
  }

  public void MainScreenKeyHit(int keyCode, int eventID)
  {
    switch (keyCode)
    {
      case KeyEvent.VK_R:
      case KeyEvent.VK_F5:
      {
        if (m_clientstarted)
        {
          // We're a remote client -- don't take local updates!
          m_ui.setWarningBorder(Color.YELLOW);
          break;
        }
        if (eventID == KeyEvent.KEY_PRESSED && m_regen_start_time == 0)
          m_regen_start_time = System.currentTimeMillis();
        else
        {
          long held = System.currentTimeMillis() - m_regen_start_time;
          if (held > 4000)
          {
            logger.info("Game Generation requested from keyboard");
            m_gen.generate();
            m_regen_start_time = System.currentTimeMillis();
          }
          if (eventID == KeyEvent.KEY_RELEASED) m_regen_start_time = 0;
        }
        break;
      }
      case KeyEvent.VK_C:
      {
        if (!m_clientstarted)
        {
          logger.info("Starting remote update client");
          m_clientstarted = true;
          m_remoteclient.setIP(m_setup.getServerHost());
          // m_remoteclient.removeRemoteUpdateListener(this);
          m_remoteclient.addRemoteUpdateListener(this);
          m_clientthread.start();
        }
        m_ui.setWarningBorder(Color.GREEN);
        break;
      }
      case KeyEvent.VK_H:
      {
        if (!m_serverstarted)
        {
          logger.info("Starting remote update server");
          m_serverstarted = true;
          m_remotethread.start();
        }
        m_ui.setWarningBorder(Color.BLUE);
        break;
      }
      case KeyEvent.VK_T:
      case KeyEvent.VK_F11:
      {
        if (eventID == KeyEvent.KEY_PRESSED)
        {
          m_paused ^= true;
          m_ui.setTimerPaused(m_paused);
          if (m_serverstarted) m_remoteupdate.setTimerPaused(m_paused);
          m_ui.setWarningBorder(m_paused ? Color.WHITE : Color.BLACK);
        }
      }
    }
  }

  public void setupChanged()
  {
    m_gen.generate();
  }

  public void RemoteError(String error, Throwable exception)
  {
    Team[] teams = new Team[1];
    GameType game = new GameType("Error");
    Map map = new Map("Remote Update");

    teams[0] = new Team("Error");
    teams[0].add(new Player(error, "Error"));

    logger.info("Updating game UI (remote error)");
    m_ui.setGame(teams, game, map);
    m_webserver.setGame(teams, game, map);
    m_ui.setWarningBorder(Color.RED);

    m_clientstarted = false;
    m_clientthread = new Thread(m_remoteclient);
  }

  public void RemoteUpdate(Team[] teams, GameType game, Map map, int delay)
  {
    if (!m_clientstarted)
    {
      logger.warning("Ignoring remote update -- in local mode!");
      return;
    }
    logger.info("Updating game UI (remote)");
    m_setup.setGameDelay(delay);
    m_ui.setGame(teams, game, map);
    m_webserver.setGame(teams, game, map);
    m_ui.setWarningBorder(Color.BLACK);
    m_remoteupdate.setGame(teams, game, map, delay); // allows proxying
  }

  public void RemotePause(boolean pauseUnpause)
  {
    m_paused = pauseUnpause;
    m_ui.setTimerPaused(m_paused);
    if (m_serverstarted) m_remoteupdate.setTimerPaused(m_paused);
    m_ui.setWarningBorder(m_paused ? Color.WHITE : Color.BLACK);
  }

}
