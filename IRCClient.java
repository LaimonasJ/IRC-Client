package ircclient;

/**
 * @author Laimonas Juras
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;



public class IRCClient
{
    private static int port = 6667;
    private static String server = "chat1.ustream.tv";
    private static String nick = "MyBot1";
    private static String login = "MyBot1";
    private static String channel = "#bot-test-ch";
    //java -jar IrcClient.jar chat1.ustream.tv #bot-test-ch 6667 MyBot2 MyBot2
    
    
    private static String ENTER = "Enter";
    static JButton enterButton;
    public static JTextArea output;
    public static JTextField input;
    public static Socket socket;
    static JFrame frame;
    static JPanel panel;
    public static String testString = "test";



    public static void createFrame()
    {
        frame = new JFrame("IRC Client");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(true);
        ButtonListener buttonListener = new ButtonListener();
        output = new JTextArea(15, 50);
        output.setWrapStyleWord(true);
        output.setEditable(false);
        JScrollPane scroller = new JScrollPane(output);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel inputpanel = new JPanel();
        inputpanel.setLayout(new FlowLayout());
        input = new JTextField(20);
        enterButton = new JButton("Enter");
        enterButton.setActionCommand(ENTER);
        enterButton.addActionListener(buttonListener);
        // enterButton.setEnabled(false);
        input.setActionCommand(ENTER);
        input.addActionListener(buttonListener);
        DefaultCaret caret = (DefaultCaret) output.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        panel.add(scroller);
        inputpanel.add(input);
        inputpanel.add(enterButton);
        panel.add(inputpanel);
        frame.getContentPane().add(BorderLayout.CENTER, panel);
        frame.pack();
        frame.setLocationByPlatform(true);
        // Center of screen
        // frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setResizable(false);
        input.requestFocusInWindow();
    }

    public static class ButtonListener implements ActionListener
    {

        @Override
        public void actionPerformed(final ActionEvent ev)
        {
            if (!input.getText().trim().equals(""))
            {
                String cmd = ev.getActionCommand();
                if (ENTER.equals(cmd))
                {
                    try
                    {
                        BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(socket.getOutputStream()));
                        writer.write(input.getText() + "\r\n");
                        writer.flush();
                        output.append("--> " + input.getText() + "\r\n");
                    }
                    catch(IOException e)
                    {
                        output.append("--!Error writing to server!--\r\n");
                    }
                }
            }
            input.setText("");
            input.requestFocusInWindow();
        }
    }

    
    
    public static void main(String[] args) throws Exception
    {

            parseArgs(args);
            
            socket = new Socket(server, port);
            
            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader (new InputStreamReader(socket.getInputStream()));

            // Log on to the server.
            writer.write("NICK " + nick + "\r\n");
            writer.write("USER " + login + " 8 * : Java test bot\r\n");
            writer.flush( );

            // Read lines from the server until it tells us we have connected.
            String line = null;
            while ((line = reader.readLine( )) != null)
            {
                System.out.println(line);
                if (line.contains("004"))
                {
                    System.out.println("LOGED IN!");
                    // We are now logged in.
                    break;
                }
                else if (line.contains("433")) {
                    System.out.println("Nickname is already in use.");
                    return;
                }
            }
            
            writer.write("JOIN " + channel + "\r\n");
            writer.flush( );
            
            createFrame();

            
            ServerListener serverListener = new ServerListener(socket, output);
            serverListener.start();
            
            serverListener.join();
            socket.close();
              
    }

    private static void parseArgs(String[] args) throws IllegalArgumentException
    {
        if (args.length == 0)
        {
            return;
        }
        if (args.length != 5)
        {
            throw new IllegalArgumentException();
        }
        server = args[0];
        channel = args[1];
        try
        {
            port = Integer.parseInt(args[2]);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException();
        }
        nick = args[3];
        login = args[4];
    }

}

class ServerListener extends Thread
{
    private InputStream in;
    private OutputStream out;
    private JTextArea output;

    public ServerListener(Socket client, JTextArea newOutput) throws IOException
    {
        in = client.getInputStream();
        out = client.getOutputStream();
        output = newOutput;
    }

    @Override
    public void run()
    {
        try
        {
            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out));
            BufferedReader reader = new BufferedReader (new InputStreamReader(in));
            while(true)
            {
                String line;
                while ((line = reader.readLine( )) != null)
                {
                    if (line.toLowerCase( ).startsWith("ping ")) {
                        // We must respond to PINGs to avoid being disconnected.
                        writer.write("PONG " + line.substring(5) + "\r\n");
                        writer.flush();
                        output.append("--!I got pinged!--\r\n");
                    }
                    else
                    {
                        // Print the raw line received by the bot.
                        output.append("<-- " + line + "\r\n");
                    }
                }
                Thread.sleep(100);
            }

        }

        catch(InterruptedException e)
        {

        }

        catch(IOException e)
        {
            e.printStackTrace();
            output.append("--!Error monitoring server!--\r\n");
        }
        finally
        {

        }
    }
}
