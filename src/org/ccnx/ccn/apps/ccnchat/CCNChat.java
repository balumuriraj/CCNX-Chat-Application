/*
 * A CCNx chat program.
 *
 * Copyright (C) 2008, 2009, 2010, 2011 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

package org.ccnx.ccn.apps.ccnchat;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.io.File;

import javax.swing.*;

import org.ccnx.ccn.apps.ccnchat.CCNChatNet.CCNChatCallback;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;


/**
 * Based on a client/server chat example in Robert Sedgewick's Algorithms
 * in Java.
 * 
 * Refactored to be just the JFrame UI.
 */
public class CCNChat extends JFrame implements ActionListener, CCNChatCallback {
	private static final long serialVersionUID = -8779269133035264361L;

    // Chat window
    protected JTextArea  _messagePane = new JTextArea(10, 32);
    private JTextField _typedText   = new JTextField(32);
    JButton openButton;
    JTextArea log;
    JFileChooser fc;
    String newline = "\n";

    private final CCNChatNet _chat;
    
    public CCNChat(String namespace) throws MalformedContentNameStringException {

    	_chat = new CCNChatNet(this, namespace);
    	
    	// close output stream  - this will cause listen() to stop and exit
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    try {
						stop();
					} catch (IOException e1) {
						System.out.println("IOException shutting down listener: " + e1);
						e1.printStackTrace();
					}
                }
            }
        );
        
        
        // Make window
        _messagePane.setEditable(false);
        _messagePane.setBackground(Color.LIGHT_GRAY);
        _messagePane.setLineWrap(true);
        _messagePane.setMargin(new Insets(5,5,5,5));
        _typedText.addActionListener(this);

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,12);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane logScrollPane = new JScrollPane(log);

        //Create a file chooser
        fc = new JFileChooser();
        
        openButton = new JButton("Transfer");
        openButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);

        Container content = getContentPane();
        JLabel emptyLabel = new JLabel("CCNxChat with Generic File Transfer!", JLabel.CENTER);
        emptyLabel.setPreferredSize(new Dimension(60, 30));
        content.add(emptyLabel, BorderLayout.NORTH);
        content.add(new JScrollPane(_messagePane), BorderLayout.CENTER);
        content.add(_typedText, BorderLayout.SOUTH);
        content.add(buttonPanel, BorderLayout.WEST);
        content.add(logScrollPane, BorderLayout.EAST);
        
        // display the window, with focus on typing box
        setTitle("CCNChat 1.2: [" + namespace + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        _typedText.requestFocusInWindow();
        setVisible(true);
    }
	
	/**
	 * Process input to TextField after user hits enter.
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		long len = 0;
        //Handle open button action.
        if (e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(CCNChat.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                //This is where a real application would open the file.
                String filepath = file.getPath();
                log.append("Transfering: " + filepath + newline);
                try {
                	int packetsize = 100000;
                	File transferFile = new File (filepath);
                	
                    len = transferFile.length();
                    int count = (int) Math.ceil((double)len/packetsize);
                    String newlength = "@FILE:"+Long.toString(len)+":"+transferFile.getName()+":"+count+":";
            		if ((null != newlength) && (newlength.length() > 0)) {
        				_chat.sendMessage(newlength);
        			}
        			
            		
                	//byte [] bytearray  = new byte [packetsize];
                    FileInputStream fin = new FileInputStream(transferFile);
                    BufferedInputStream bin = new BufferedInputStream(fin);
                    
                    while(count > 1) {
                    	byte [] bytearray  = new byte [packetsize];
                    bin.read(bytearray,0,bytearray.length);
                    //String newText = new String(bytearray);
                    String newText = new sun.misc.BASE64Encoder().encodeBuffer(bytearray);
                    if ((null != newText) && (newText.length() > 0)) {
        				_chat.sendMessage(newText);
        			}
                    count--;
                    }
                    byte [] byte_array  = new byte [(int)len%packetsize];
                    bin.read(byte_array,0,(int)len%packetsize);
                    //String newText = new String(bytearray);
                    String newText = new sun.misc.BASE64Encoder().encodeBuffer(byte_array);
                    System.out.println((int)len%packetsize+":"+newText.length());
                    if ((null != newText) && (newText.length() > 0)) {
        				_chat.sendMessage(newText);
        			}
                    count--;
                    
        			bin.close();
        			count =0;

        		} catch (Exception e1) {
        			System.err.println("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
        			e1.printStackTrace();
        			recvMessage("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
        		}
                //log.append("Transfer completed! " + len + " Bytes transfered!" + newline);
            } else {
                log.append("Open command cancelled by user." + newline);
            }
            log.setCaretPosition(log.getDocument().getLength());
        } 
        else {
		try {
			String newText = _typedText.getText();
			if ((null != newText) && (newText.length() > 0)) {
				_chat.sendMessage(newText);
			}

		} catch (Exception e1) {
			System.err.println("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
			e1.printStackTrace();
			recvMessage("Exception saving our input: " + e1.getClass().getName() + ": " + e1.getMessage());
		}
        _typedText.setText("");
        _typedText.requestFocusInWindow();
        }
	}

	
	/**
	 * Add a message to the output.
	 * @param message
	 */
	
	public void recvMessage(String message) {
		_messagePane.insert(message, _messagePane.getText().length());
        _messagePane.setCaretPosition(_messagePane.getText().length());
	}
	String file_name = "";
	long s =0;
	public void recvMessage(String message, long size, String filename, int count, int counter) {
		try {
					System.out.println(message.length());
					byte[] store =	new sun.misc.BASE64Decoder().decodeBuffer(message);
					System.out.println(store.length);
					File f = new File(filename);
					if(f.exists() && counter!=count-1){
					//FileOutputStream fos = new FileOutputStream(filename);
					FileOutputStream fos = 	new FileOutputStream(f, true);
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					bos.write(store, 0 , store.length);
					bos.flush();
					bos.close();
					if(counter < 1){
					//log.append("File recieved!" + size + " Bytes recieved" + newline);
					//log.append("Path: " + f.getPath() + newline);
						log.append("Transfer completed! " + size + " Bytes transfered!" + newline);
					counter = 0;
					}
					}
					else 
					{
						FileOutputStream fos = new FileOutputStream(filename);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						bos.write(store, 0 , store.length);
						bos.flush();
						bos.close();
						if(counter < 1){
							//log.append("File recieved! " + size + " Bytes recieved" + newline);
							//log.append("Path: " + f.getPath() + newline);
							log.append("Transfer completed! " + size + " Bytes transfered!" + newline);
						}
					}
					
				
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
    public static void usage() {
    	System.err.println("usage: CCNChat <ccn URI>");
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(-1);
		}
		CCNChat client;
		//Scanner obj = new Scanner(System.in);
		try {
			client = new CCNChat(args[0]);
			//String str = obj.nextLine();
			//client = new CCNChat(str);
			client.start();
		} catch (MalformedContentNameStringException e) {
			System.err.println("Not a valid ccn URI: " + args[0] + ": " + e.getMessage());
			e.printStackTrace();
		} catch (ConfigurationException e) {
			System.err.println("Configuration exception running ccnChat: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException handling chat messages: " + e.getMessage());
			e.printStackTrace();
		}
		//remove
		//obj.close();
	}
	
	// =========================================================
	// Internal methods
	
	/**
	 * Called by window thread when when window closes
	 */
	protected void stop() throws IOException {
		_chat.shutdown();
	}
	
	/**
	 * This blocks until _chat.shutdown() called
	 * @throws IOException 
	 * @throws MalformedContentNameStringException 
	 * @throws ConfigurationException 
	 */
	protected void start() throws ConfigurationException, MalformedContentNameStringException, IOException {
		_chat.listen();
	}
}
