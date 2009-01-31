/**
 * @file SimpleWebInterface.java by Kyle Lemons, created Jan 27, 2009
 */

package net.kylelemons.halo3;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kylelemons.halo3.GameList.GameType;
import net.kylelemons.halo3.MapList.Map;

/**
 * @author eko
 * 
 */
public class RemoteUpdateClient implements Runnable
{
  private static Logger                   logger                   = Logger.getLogger("net.kylelemons.halo3");

  public static final int                 DEFAULT_PORT             = 1881;                                    // TODO
  // configurable
  public static final int                 MAX_LIFETIME_CONNECTIONS = 1024;
  private Team[]                          m_teams;
  private GameType                        m_game;
  private Map                             m_map;
  private String                          m_hostname;

  private ArrayList<RemoteUpdateListener> m_listeners;

  private int                             m_delay;

  public static interface RemoteUpdateListener
  {
    /**
     * Gets called when there is a remote update from the server
     * 
     * @param teams
     *          The new teams received
     * @param game
     *          The game received
     * @param map
     *          The map received
     * @param delay
     *          The delay between games, in seconds
     */
    void RemoteUpdate(Team[] teams, GameType game, Map map, int delay);

    /**
     * Gets called when there is a problem connecting
     * 
     * @param error
     *          Text describing the error
     * @param exception
     *          The exception condition... this may be null
     */
    void RemoteError(String error, Throwable exception);
  }

  RemoteUpdateClient(int port)
  {
    m_listeners = new ArrayList<RemoteUpdateListener>();
  }

  RemoteUpdateClient()
  {
    this(DEFAULT_PORT);
  }

  public void run()
  {
    Socket socket;
    ObjectInputStream in;
    BufferedInputStream buffer;

    try
    {
      socket = new Socket("127.0.0.1", RemoteUpdateServer.DEFAULT_PORT);

      buffer = new BufferedInputStream(socket.getInputStream());
      in = new ObjectInputStream(buffer);

      logger.info("Successfully connected to " + m_hostname);

      Object obj;
      while (socket.isConnected() && !Thread.interrupted())
      {
        obj = in.readObject();
        if (obj.getClass() == String.class)
        {
          String cmd = (String) obj;
          if (cmd.equals("Update"))
          {
            m_teams = (Team[]) in.readObject();
            m_game = (GameType) in.readObject();
            m_map = (Map) in.readObject();
            m_delay = (Integer) in.readObject();
            logger.info("Completed Update");
            fireRemoteUpdate();
          }
        }
        else
        {
          logger.info("Recieved unexpected object: " + obj.getClass().getName());
        }
      }
      if (Thread.interrupted())
        logger.info("Client stopping -- Interrupted");
      else
        logger.info("Client stopping -- Socket closed");
    }
    catch (ConnectException e)
    {
      logger.log(Level.WARNING, "Could not connect to host: " + m_hostname, e);
      fireRemoteError("Connection Refused: " + m_hostname, e);
    }
    catch (UnknownHostException e)
    {
      logger.log(Level.SEVERE, "Could not resolve host: " + m_hostname, e);
      fireRemoteError("Unresolvable Host: " + m_hostname, e);
    }
    catch (EOFException e)
    {
      logger.info("Server disconnected");
      fireRemoteError("Server Disconnected", e);
    }
    catch (IOException e)
    {
      logger.log(Level.SEVERE, "IO Exception", e);
    }
    catch (ClassNotFoundException e)
    {
      logger.log(Level.SEVERE, "Class Not Found Exception", e);
    }
    catch (ClassCastException e)
    {
      logger.log(Level.SEVERE, "Class Cast Exception", e);
    }
  }

  public void setIP(String remoteHost)
  {
    m_hostname = remoteHost;
  }

  private void fireRemoteUpdate()
  {
    ArrayList<Integer> deleteIndices = new ArrayList<Integer>();
    logger.info("Firing remote update to " + m_listeners.size() + " listeners...");
    for (int i = 0; i < m_listeners.size(); ++i)
    {
      try
      {
        m_listeners.get(i).RemoteUpdate(m_teams, m_game, m_map, m_delay);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        logger.severe("Exception in listener! Deleting.");
        deleteIndices.add(i);
      }
    }
    // Remove from last to first to maintain indices
    for (int d = deleteIndices.size() - 1; d >= 0; ++d)
    {
      m_listeners.remove(deleteIndices.get(d));
    }
  }

  private void fireRemoteError(String error, Throwable exception)
  {
    ArrayList<Integer> deleteIndices = new ArrayList<Integer>();
    for (int i = 0; i < m_listeners.size(); ++i)
    {
      try
      {
        m_listeners.get(i).RemoteError(error, exception);
      }
      catch (Exception e)
      {
        e.printStackTrace();
        logger.severe("Exception in listener! Deleting.");
        deleteIndices.add(i);
      }
    }
    // Remove from last to first to maintain indices
    for (int d = deleteIndices.size() - 1; d >= 0; ++d)
    {
      m_listeners.remove(deleteIndices.get(d));
    }
  }

  public void addRemoteUpdateListener(RemoteUpdateListener rul)
  {
    m_listeners.add(rul);

    Team[] teams = new Team[3];
    GameType game = new GameType("Waiting");
    Map map = new Map("Remote Update");

    teams[0] = new Team("Waiting");
    teams[1] = new Team("for");
    teams[2] = new Team("Update");

    rul.RemoteUpdate(teams, game, map, 1);
  }

  public void removeRemoteUpdateListener(RemoteUpdateListener rul)
  {
    m_listeners.remove(rul);
  }
}
