/**
 * @file GameSetup.java by [AUTHOR], created Jan 21, 2009
 */
package net.kylelemons.halo3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author eko
 * 
 */
public class GameSetup implements Serializable
{
  private static final long serialVersionUID         = 8786786673968199764L;
  private static final int  CURRENT_DATABASE_VERSION = 2;

  public static interface SetupChangeListener
  {
    public void setupChanged();
  }

  public static final int                          MAX_ALLOWED_TEAMS = 8;
  public static final int                          MAX_TEAM_SIZE     = 8;
  private static Logger                            logger            = Logger.getLogger("net.kylelemons.halo3");

  private int                                      m_fairness;
  private String[]                                 m_teamnames;
  private int[]                                    m_teamsizes;
  private String                                   m_serverhost;
  private transient ArrayList<SetupChangeListener> m_listeners;
  private int                                      m_gamedelay;
  private int                                      m_fair_teams;
  private PlayerList                               m_playerlist;
  private GameList                                 m_gamelist;
  private MapList                                  m_maplist;
  private int                                      m_dbversion;

  public GameSetup()
  {
    m_dbversion = CURRENT_DATABASE_VERSION;
    m_fairness = 1;
    m_gamedelay = 60 * 7;
    m_serverhost = "127.0.0.1";
    m_listeners = new ArrayList<SetupChangeListener>();
    m_teamnames = new String[MAX_ALLOWED_TEAMS];
    for (int i = 0; i < MAX_ALLOWED_TEAMS; ++i)
      m_teamnames[i] = TeamGrid.TeamNames[i];
    m_teamsizes = new int[MAX_ALLOWED_TEAMS];
    for (int i = 0; i < MAX_ALLOWED_TEAMS; ++i)
      m_teamsizes[i] = 0;
  }

  public MapList getMapList()
  {
    return m_maplist;
  }

  public void setMapList(MapList mapList)
  {
    this.m_maplist = mapList;
  }

  public GameList getGameList()
  {
    return m_gamelist;
  }

  public void setGameList(GameList gameList)
  {
    this.m_gamelist = gameList;
  }

  public PlayerList getPlayerList()
  {
    return m_playerlist;
  }

  public void setPlayerList(PlayerList playerlist)
  {
    this.m_playerlist = playerlist;
  }

  public void setTeamName(int team, String name)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return;
    m_teamnames[team] = name;
    this.fireSetupChange();
  }

  public String getTeamName(int team)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return null;
    return m_teamnames[team];
  }

  /**
   * @param fairness
   *          the fairness to set
   */
  public void setFairness(int fairness)
  {
    this.m_fairness = fairness;
    fireSetupChange();
  }

  /**
   * @return the fairness
   */
  public int getFairness()
  {
    return m_fairness;
  }

  public void addSetupChangeListener(SetupChangeListener c)
  {
    m_listeners.add(c);
  }

  public void removeSetupChangeListener(SetupChangeListener c)
  {
    m_listeners.remove(c);
  }

  private void fireSetupChange()
  {
    for (int i = 0; i < m_listeners.size(); ++i)
      m_listeners.get(i).setupChanged();
  }

  public void setTeamSize(int team, int maxPlayers)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return;
    m_teamsizes[team] = maxPlayers;
    this.fireSetupChange();
  }

  public int getTeamSize(int team)
  {
    if (team >= MAX_ALLOWED_TEAMS || team < 0) return -1;
    return m_teamsizes[team];
  }

  /**
   * This does not fire the setup changed, as it shouldn't really affect the generated game
   * 
   * @param seconds
   *          How many seconds to wait in between games
   */
  public void setGameDelay(int seconds)
  {
    m_gamedelay = seconds;
  }

  public int getGameDelay()
  {
    return m_gamedelay;
  }

  public void setFairTeamCount(int i)
  {
    m_fair_teams = i;
    fireSetupChange();
  }

  public int getFairTeamCount()
  {
    // TODO Auto-generated method stub
    return m_fair_teams;
  }

  public String getServerHost()
  {
    return m_serverhost;
  }

  public void setServerHost(String serverHost)
  {
    this.m_serverhost = serverHost;
  }

  public String loadDatabase(String filename)
  {
    if (filename.length() <= 0) return "Zero-length filenames are unacceptable";

    try
    {
      InputStream file = new FileInputStream(filename);
      InputStream buffer = new BufferedInputStream(file);
      ObjectInput input = new ObjectInputStream(buffer);
      try
      {
        // Read the database version
        Integer dbVersion = (Integer) input.readObject();

        // Read in the player count
        Object nextObject = input.readObject();

        // If this is a zero database, the next will be a player instead of the count
        if (nextObject instanceof PlayerList.Player)
        {
          // so use the version as the player count and set the db version to zero
          nextObject = dbVersion;
          dbVersion = 0;
        }

        // Print out version information
        logger.info("Database: Loading v" + dbVersion + " (current: v" + CURRENT_DATABASE_VERSION + ")");

        // Get the player count
        Integer playerCount = (Integer) nextObject;
        m_playerlist.clear();

        logger.info("Loading " + playerCount + " players...");
        for (int i = 0; i < playerCount; ++i)
        {
          PlayerList.Player next = (PlayerList.Player) input.readObject();
          m_playerlist.add(next);
        }

        Integer mapCount = (Integer) input.readObject();
        m_maplist.clear();

        logger.info("Loading " + mapCount + " maps...");
        for (int i = 0; i < mapCount; ++i)
        {
          MapList.Map next = (MapList.Map) input.readObject();
          m_maplist.add(next);
        }
        // m_maplist.add(new MapList.Map("Assembly", "images/Assembly.jpg", 2, 8, 3));
        // m_maplist.add(new MapList.Map("Orbital", "images/Orbital.jpg", 4, 12, 3));
        // m_maplist.add(new MapList.Map("The Pit", "images/The Pit.jpg", 4, 10, 3));
        // m_maplist.add(new MapList.Map("Sandbox", "images/Sandbox.jpg", 4, 12, 3));
        // m_maplist.add(new MapList.Map("Standoff", "images/Standoff.jpg", 4, 12, 3));

        Integer gameTypeCount = (Integer) input.readObject();
        m_gamelist.clear();

        logger.info("Loading " + gameTypeCount + " games...");
        for (int i = 0; i < gameTypeCount; ++i)
        {
          GameList.GameType next = (GameList.GameType) input.readObject();
          m_gamelist.add(next);
        }

        logger.info("Loading Game Setup");
        m_fairness = ((Integer) input.readObject());
        m_gamedelay = ((Integer) input.readObject());
        if (dbVersion <= 1) input.readObject(); // Discard team count
        m_fair_teams = ((Integer) input.readObject());
        m_serverhost = ((String) input.readObject());
        for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
        {
          setTeamSize(i, (Integer) input.readObject());
          setTeamName(i, (String) input.readObject());
        }

        logger.info("Database loaded successfully");
      }
      catch (ClassCastException ex)
      {
        logger.log(Level.SEVERE, "Attempted to read the wrong class!", ex);
      }
      finally
      {
        input.close();
      }
    }
    catch (FileNotFoundException ex)
    {
      logger.log(Level.SEVERE, "Could not load file: " + filename, ex);
    }
    catch (ClassNotFoundException ex)
    {
      logger.log(Level.SEVERE, "Cannot perform input. Class not found.", ex);
    }
    catch (IOException ex)
    {
      logger.log(Level.SEVERE, "Cannot perform input.", ex);
    }

    return "Successfully loaded from database";
  }

  public String saveDatabase(String filename)
  {
    if (filename.length() <= 0)
    {
      return "Empty filenames are not acceptable.";
    }

    try
    {
      // use buffering
      OutputStream file = new FileOutputStream(filename);
      OutputStream buffer = new BufferedOutputStream(file);
      ObjectOutput output = new ObjectOutputStream(buffer);
      try
      {
        // Write the database version
        output.writeObject(m_dbversion);

        Integer playerCount = new Integer(m_playerlist.getSize());
        output.writeObject(playerCount);

        logger.info("Writing " + playerCount + " players...");

        for (int i = 0; i < playerCount; ++i)
          output.writeObject(m_playerlist.getPlayers().get(i));

        Integer mapCount = new Integer(m_maplist.getSize());
        output.writeObject(mapCount);

        logger.info("Writing " + mapCount + " maps...");
        for (int i = 0; i < mapCount; ++i)
          output.writeObject(m_maplist.getMapList().get(i));

        // Uncomment to add a new gametype... Major kludge, TODO add new
        // m_gamelist.add(new GameList.GameType("Team Oddball", 3));

        Integer gameTypeCount = new Integer(m_gamelist.getSize());
        output.writeObject(gameTypeCount);

        logger.info("Writing " + gameTypeCount + " games...");
        for (int i = 0; i < gameTypeCount; ++i)
          output.writeObject(m_gamelist.getGameList().get(i));

        logger.info("Writing Game Setup");
        output.writeObject(m_fairness);
        output.writeObject(m_gamedelay);
        output.writeObject(m_fair_teams);
        output.writeObject(m_serverhost);
        for (int i = 0; i < GameSetup.MAX_ALLOWED_TEAMS; ++i)
        {
          output.writeObject(getTeamSize(i));
          output.writeObject(getTeamName(i));
        }

        logger.info("Database written successfully");
      }
      finally
      {
        output.close();
      }
    }
    catch (IOException ex)
    {
      logger.log(Level.SEVERE, "Cannot perform output.", ex);
      return "Error in writing";
    }
    return "Successfully wrote to database";
  }

  public int countActiveTeams()
  {
    int active = 0;
    for (int i = 0; i < MAX_ALLOWED_TEAMS; ++i)
      if (m_teamsizes[i] > 0) active++;
    return active;
  }

}
