/**
 * @file SimpleWebInterface.java by Kyle Lemons, created Jan 27, 2009
 */

package net.kylelemons.halo3;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kylelemons.halo3.GameList.GameType;
import net.kylelemons.halo3.MapList.Map;

/**
 * @author eko
 * 
 */
public class RemoteUpdateServer implements Runnable
{
  private static Logger                 logger                   = Logger.getLogger("net.kylelemons.halo3");

  public static final int               DEFAULT_PORT             = 1881;                                    // TODO
  // configurable
  public static final int               MAX_LIFETIME_CONNECTIONS = 1024;
  private int                           m_port;
  private Team[]                        m_teams;
  private GameType                      m_game;
  private Map                           m_map;
  private ArrayList<UpdateClientThread> m_clients;

  private ArrayList<Thread>             m_threads;

  private int                           m_delay;

  class UpdateClientThread implements Runnable
  {
    private Socket  m_client;
    private Boolean m_send_update;

    UpdateClientThread(Socket client)
    {
      this.m_client = client;
      m_send_update = false;
    }

    public void run()
    {

      try
      {
        // Get input from the client
        BufferedOutputStream buffer = new BufferedOutputStream(m_client.getOutputStream());
        ObjectOutput out = new ObjectOutputStream(buffer);

        m_send_update = true;

        logger.info("Client connected: " + m_client.getInetAddress().getHostAddress());

        try
        {
          do
          {
            synchronized (m_send_update)
            {
              m_send_update.wait();
            }
            if (!m_client.isConnected()) break;
            if (m_teams != null)
            {
              // Send Teams
              out.writeObject("Update");
              out.writeObject(m_teams);
              out.writeObject(m_game);
              out.writeObject(m_map);
              out.writeObject(m_delay);
              out.flush();
              logger.info("Sent update to client");
            }
          }
          while (m_client.isConnected() && !Thread.interrupted());
        }
        catch (InterruptedException e)
        {
          logger.log(Level.SEVERE, "Interrupted", e);
        }
        finally
        {
          m_client.close();
        }
      }
      catch (IOException ioe)
      {
        logger.log(Level.SEVERE, "IO Exception", ioe);
      }
    }

    public void sendUpdate()
    {
      synchronized (m_send_update)
      {
        if (m_send_update) m_send_update.notifyAll();
      }
    }
  }

  RemoteUpdateServer(int port)
  {
    m_port = port;
  }

  RemoteUpdateServer()
  {
    this(DEFAULT_PORT);
  }

  public void run()
  {
    int i = 0;

    try
    {
      ServerSocket listener = new ServerSocket(m_port);
      Socket server;

      m_clients = new ArrayList<UpdateClientThread>();
      m_threads = new ArrayList<Thread>();

      logger.info("Server entering accept loop");
      while ((i++ < MAX_LIFETIME_CONNECTIONS) || (MAX_LIFETIME_CONNECTIONS == 0) && !Thread.interrupted())
      {
        server = listener.accept();
        UpdateClientThread conn_c = new UpdateClientThread(server);
        Thread t = new Thread(conn_c);
        t.start();
        m_clients.add(conn_c);
        m_threads.add(t);
      }
      if (!Thread.interrupted())
        logger.severe("Maximum number of lifetime connections exceeded");
      else
        logger.info("Server stopping -- Interrupted");
    }
    catch (IOException ioe)
    {
      logger.log(Level.SEVERE, "IO Exception", ioe);
    }
  }

  public void setGame(Team[] teams, GameType game, Map map, int delay)
  {
    if (m_clients == null) return;
    m_teams = teams;
    m_game = game;
    m_map = map;
    m_delay = delay;
    for (int i = 0; i < m_clients.size(); ++i)
    {
      if (m_threads.get(i).isAlive()) m_clients.get(i).sendUpdate();
    }
  }
}
