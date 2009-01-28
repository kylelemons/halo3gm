package net.kylelemons.halo3;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kylelemons.halo3.GameSetup.SetupChangeListener;

public class HaloGameMaster implements GameGenerator.GameChangedListener, UserInterface.KeyListener, SetupChangeListener
{
  private UserInterface m_ui;
  private GameGenerator m_gen;
  private PlayerList m_players;
  private GameList m_games;
  private MapList m_maps;
  
  private GameSetup m_setup;
  
  // TODO Logging
  public static final String APPLICATION = "HaloGameMaster";
  public static final String VERSION = "v0.1.4";
  private long m_regen_start_time;
  private SimpleWebInterface m_webserver;
  private Thread m_webthread;

  private static Logger logger = Logger.getLogger("net.kylelemons.halo3");
  
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
    logger.addHandler(fh); //fh.setFormatter(new SimpleFormatter());
    
    // Request that every detail gets logged.
    logger.setLevel(Level.ALL);
    
    // Log a simple INFO message.
    logger.info(APPLICATION+"-"+VERSION+" Starting...");
    
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
      m_games.add(new GameList.GameType("Team Slayer",   5));
      m_games.add(new GameList.GameType("Team CTF",      2, false));
      m_games.add(new GameList.GameType("Team King",     2));
      m_games.add(new GameList.GameType("VIP",           1));
      m_games.add(new GameList.GameType("Zombies",       1, false));
      m_games.add(new GameList.GameType("Rocket Swords", 1, false));
      m_games.add(new GameList.GameType("Shotty Sniper", 1));
      m_games.add(new GameList.GameType("Ninja",         1, false));
      m_games.sort();
    m_ui.setGameList(m_games);
    m_gen.setGameList(m_games);
    
    m_maps = new MapList();
      m_maps.add(new MapList.Map("Blackout",      "images/Blackout.jpg",      4, 10, 3));
      m_maps.add(new MapList.Map("Cold Storage",  "images/Cold_Storage.jpg",  2,  6, 3));
      m_maps.add(new MapList.Map("Foundry",       "images/Foundry.jpg",       4, 10, 3));
      m_maps.add(new MapList.Map("Ghost Town",    "images/Ghost_Town.jpg",    4, 12, 3));
      m_maps.add(new MapList.Map("Guardian",      "images/Guardian.jpg",      2,  8, 3));
      m_maps.add(new MapList.Map("Epitaph",       "images/Epitaph.jpg",       4, 10, 2));
      m_maps.add(new MapList.Map("Narrows",       "images/Narrows.jpg",       4, 10, 2));
      m_maps.add(new MapList.Map("Avalanche",     "images/Avalanche.jpg",    10, 16, 1));
      m_maps.add(new MapList.Map("Construct",     "images/Construct.jpg",     8, 12, 1));
      m_maps.add(new MapList.Map("High Ground",   "images/High_Ground.jpg",   6, 12, 1));
      m_maps.add(new MapList.Map("Isolation",     "images/Isolation.jpg",     8, 10, 1));
      m_maps.add(new MapList.Map("Last Resort",   "images/Last_Resort.jpg",   4, 16, 1));
      m_maps.add(new MapList.Map("Rat's Nest",    "images/Rat's_Nest.jpg",    6, 16, 1));
      m_maps.add(new MapList.Map("Sandtrap",      "images/Sandtrap.jpg",     10, 16, 1));
      m_maps.add(new MapList.Map("Snowbound",     "images/Snowbound.jpg",    12, 16, 1));
      m_maps.add(new MapList.Map("Valhalla",      "images/Valhalla.jpg",     10, 16, 1));
      m_maps.sort();
    m_ui.setMapList(m_maps);
    m_gen.setMapList(m_maps);
    
    logger.fine("Running UI Setup");
    m_ui.setup(); // the last thing this does is load the database, overwriting the above
    
    m_setup.addSetupChangeListener(this);
    
    logger.info("Setting up web server");
    m_webserver = new SimpleWebInterface();
    
    logger.info("Setup Complete");
  }
  
  public void start()
  {
    logger.info("Starting "+APPLICATION);
    m_gen.addGameChangedListener(this);
    m_ui.addKeyListener(this);
    m_gen.generate();
    
    logger.info("Starting webserver");
    m_webthread = new Thread(m_webserver);
    m_webthread.start();
  }

  public void gameChanged(GameGenerator gen)
  {
    logger.info("Updating game UI");
    m_ui.setGame(gen.getTeams(), gen.getGame(), gen.getMap());
    m_webserver.setGame(gen.getTeams(), gen.getGame(), gen.getMap());
    m_ui.setWarningBorder(gen.getGood());
  }

  public void MainScreenKeyHit(int keyCode, int eventID)
  {
    switch (keyCode)
    {
      case KeyEvent.VK_R:
      case KeyEvent.VK_F5:
      {
        if (eventID == KeyEvent.KEY_PRESSED && m_regen_start_time == 0)
          m_regen_start_time = System.currentTimeMillis();
        else
        {
          long held = System.currentTimeMillis()-m_regen_start_time;
          if (held > 4000)
          {
            logger.info("Game Generation requested from keyboard");
            m_gen.generate();
            m_regen_start_time = System.currentTimeMillis();
          }
          if (eventID == KeyEvent.KEY_RELEASED)
            m_regen_start_time = 0;
        }
        
      }
    }
  }

  public void setupChanged()
  {
    m_gen.generate();
  }

}
