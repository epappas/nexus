/**
 *
 */
package com.evalon.chrysalis.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

/** @author Evangelos Pappas - Evalon.gr */
public class SocketReactorOld implements Runnable {
	private Thread thread;
	private boolean isRunning;
	private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap<SocketChannel, List<byte[]>>();
	private ByteBuffer buffer = ByteBuffer
			                            .allocate(2 * 1024);

	public SocketReactorOld() {
		this.thread = new Thread(this);
		this.isRunning = false;
	}

	public void start() {
		this.isRunning = true;
		this.thread.start();
	}

	@Override
	public void run() {
		try {
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			//channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			//channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
			channel.setOption(StandardSocketOptions.SO_RCVBUF, 256 * 1024);
			channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			channel.socket().bind(new InetSocketAddress("0.0.0.0", 35355));
			Selector selector = Selector.open();
			channel.register(selector, SelectionKey.OP_ACCEPT, null);
			while (this.isRunning) {
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys()
						                                  .iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if (!key.isValid()) {
						continue;
					}
					if (key.isAcceptable()) {
						this.doAccept(key);
					} else if (key.isReadable()) {
						this.doRead(key);
					} else if (key.isWritable()) {
						this.doWrite(key);
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);
		System.out.println("Incoming connection from: "
				                   + socketChannel.getRemoteAddress());
		// write a welcome message
		socketChannel.write(ByteBuffer.wrap("Hello!\n".getBytes("UTF-8")));
		// register channel with selector for further I/O
		keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
		socketChannel.register(key.selector(), SelectionKey.OP_READ);
	}

	// isReadable returned true
	private void doRead(SelectionKey key) {
		try {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			buffer.clear();
			int numRead = -1;
			try {
				numRead = socketChannel.read(buffer);
			}
			catch (IOException e) {
				System.err.println("Cannot read error!");
			}
			if (numRead == -1) {
				this.keepDataTrack.remove(socketChannel);
				System.out.println("Connection closed by: "
						                   + socketChannel.getRemoteAddress());
				socketChannel.close();
				key.cancel();
				return;
			}
			byte[] data = new byte[numRead];
			System.arraycopy(buffer.array(), 0, data, 0, numRead);
			System.out.println(new String(data, "UTF-8") + " from "
					                   + socketChannel.getRemoteAddress());
			// write back to client
			doEchoJob(key, data);
		}
		catch (IOException ex) {
			System.err.println(ex);
		}
	}

	// isWritable returned true
	private void doWrite(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		Iterator<byte[]> its = channelData.iterator();

		while (its.hasNext()) {
			byte[] it = its.next();
			its.remove();
			socketChannel.write(ByteBuffer.wrap(it));
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	private void doEchoJob(SelectionKey key, byte[] data) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		channelData.add(data);
		key.interestOps(SelectionKey.OP_WRITE);
	}

	public static void main(String[] args) throws InterruptedException {
		SocketReactorOld socketReactorOld = new SocketReactorOld();
		socketReactorOld.start();

		Thread.sleep(1000);

		new Thread(new Runnable() {
			@Override
			public void run() {
				final int DEFAULT_PORT = 35355;
				final String IP = "127.0.0.1";
				ByteBuffer buffer = ByteBuffer.allocateDirect(2 * 1024);
				ByteBuffer randomBuffer;
				CharBuffer charBuffer;
				Charset charset = Charset.defaultCharset();
				CharsetDecoder decoder = charset.newDecoder();
				// open Selector and ServerSocketChannel by calling the open()
				// method
				try (Selector selector = Selector.open();
				     SocketChannel socketChannel = SocketChannel.open()) {
					// check that both of them were successfully opened
					if ((socketChannel.isOpen()) && (selector.isOpen())) {
						// configure non-blocking mode
						socketChannel.configureBlocking(false);
						// set some options
						socketChannel.setOption(
								                       StandardSocketOptions.SO_RCVBUF, 128 * 1024);
						socketChannel.setOption(
								                       StandardSocketOptions.SO_SNDBUF, 128 * 1024);
						socketChannel.setOption(
								                       StandardSocketOptions.SO_KEEPALIVE, true);
						// register the current channel with the given selector
						socketChannel.register(selector,
								                      SelectionKey.OP_CONNECT);
						// connect to remote host
						socketChannel.connect(new java.net.InetSocketAddress(
								                                                    IP, DEFAULT_PORT));
						System.out.println("Localhost: "
								                   + socketChannel.getLocalAddress());
						// waiting for the connection
						while (selector.select(1000) > 0) {
							// get keys
							Set<?> keys = selector.selectedKeys();
							Iterator<?> its = keys.iterator();
							// process each key
							while (its.hasNext()) {
								SelectionKey key = (SelectionKey) its.next();
								// remove the current key
								its.remove();
								// get the socket channel for this key
								try (SocketChannel keySocketChannel = (SocketChannel) key
										                                                      .channel()) {
									// attempt a connection
									if (key.isConnectable()) {
										// signal connection success
										System.out.println("I am connected!");
										// close pending connections
										if (keySocketChannel
												    .isConnectionPending()) {
											keySocketChannel.finishConnect();
										}
										// read/write from/to server
										while (keySocketChannel.read(buffer) != -1) {
											buffer.flip();
											charBuffer = decoder.decode(buffer);
											System.out.println(charBuffer
													                   .toString());
											if (buffer.hasRemaining()) {
												buffer.compact();
											} else {
												buffer.clear();
											}
											int r = new Random().nextInt(100);
											if (r == 50) {
												System.out
														.println("50 was generated! Close the socket channel!");
												break;
											} else {
												randomBuffer = ByteBuffer.wrap("Random number:"
														                               .concat(String
																                                       .valueOf(r))
														                               .getBytes("UTF-8"));
												keySocketChannel
														.write(randomBuffer);
												try {
													Thread.sleep(1500);
												}
												catch (InterruptedException ex) {
												}
											}
										}
									}
								}
								catch (IOException ex) {
									System.err.println(ex);
								}
							}
						}
					} else {
						System.out
								.println("The socket channel or selector cannot be opened!");
					}
				}
				catch (IOException ex) {
					System.err.println(ex);
				}

			}
		}).start();
	}
}
