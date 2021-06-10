package flappybird;

//Importando bibliotecas do proprio java
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

import java.io.File;
import java.io.IOException;

//Bibliotecas que eu criei
import entities.*;
import graficos.*;
import world.*;

public class Game extends Canvas implements Runnable,KeyListener{

    //Adicionando um "serial number" ao canvas
	private static final long serialVersionUID = 1L;

	//Declarando atributos da minha classe
	
	private JFrame frame; 							// Janela do meu jogo
	
	private static boolean isRunning; 				// Variavel para manter o jogo ligado
	
	private final static int WIDTH  = 240; 			// Comprimento da janela a ser criada
	private final static int HEIGHT = 160; 			// Altura da janela a ser criada
	public final static int  SCALE  = 5; 			// x vezes que a janela sera aumentada
	private static Thread    thread;				// Criando threads
	
	public static BufferedImage     layer; 			// Imagem de fundo do meu jogo
	public static ArrayList<Entity> entities;		// array dinamico contendo todos as entidades
	public static Spritesheet       spritesheet; 	// Spritesheet com todos os sprites
	
	public static World  world;						// Objeto contendo o mapa do mundo
	public static Player player;					// Objeto player
	
	public static Sound  sound;
	
	public static UI ui;							// Interface de usuario	
	public static PipeGenerator obstacles;
	
	public static Gamestate gamestate;

	private boolean showMessageGameOver = true;
	private boolean restartGame         = false;

	private int framesGameOver          = 0;
	
	//Metodo para pegar o valor em WIDTH
	public static int getWIDTH() {
		return WIDTH;
	}
	
	//Metodo para pegar o valor e HEIGHT
	public static int getHEIGHT() {
		return HEIGHT;
	}
	
	//Construtor do jogo
	public Game() {
		
		gamestate = Gamestate.ingame;
		sound     = new Sound();
		ui        = new UI(WIDTH,HEIGHT,SCALE);
		obstacles = new PipeGenerator();
		
		//Em janela
		this.setPreferredSize(new Dimension(WIDTH*SCALE,HEIGHT*SCALE));
		
		//Configurando as preferencias da minha janela
		initFrame();
		
		//Iniciando classe para permitir o controle do jogo
		this.addKeyListener(this);
		
		initObjects();
		
	}
	
	public static void initObjects() {
		
		try {
			//Criando um array flexivel para todas as entidades e inimigos do jogo
			entities = new ArrayList<Entity>();
	
			//Criando o objeto que contem o spritesheet do jogo
			spritesheet = new Spritesheet("/spritesheet.png");
			
			//Criando o player, e adicionando ele na ArrayList
			player = new Player(WIDTH/2 - 13,HEIGHT/2 - 5 ,16,16,2,spritesheet.getSprite(0,0,16,16));
			entities.add(player);

		} catch (Exception e) {e.printStackTrace();}
	}
	
	//Metodo para iniciar a janela
	private void initFrame() {
		
		//Configurando janela do jogo
		this.frame = new JFrame("Flappy Bird"); 

		frame.setResizable(false); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		frame.add(this);
		//frame.setUndecorated(true);
		frame.pack();
		
		//Configurando icone da janela

		Image icon = null;

		try {
			File tmp = new File("/icon.png");
			
			if( tmp.exists() && !tmp.isDirectory() ) {
				icon = ImageIO.read(getClass().getResource("/icon.png"));
				frame.setIconImage(icon);
				
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image cursor    = toolkit.getImage(getClass().getResource("/icon.png"));
				Cursor c        = toolkit.createCustomCursor(cursor, new Point(0,0), "img");
				frame.setCursor(c);
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		//Configurando a imagem de fundo
		layer = new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
		
	}
	
	//Primeira funcao a ser chamada no programa
	public static void main(String[] args) throws IOException, FontFormatException {
		
		//Iniciando o nosso jogo
		Game game = new Game();
		game.start();
	}
	
	public synchronized void start() {
		
		//Iniciando as minhas threads
		thread = new Thread(this);
		
		//Meu jogo foi iniciado, logo, sera igual a "true"
		isRunning = true;
		
		//Iniciando as Threads
		thread.start();
		
	}
	
	public void run() {
		
		//Fazendo com que o jogo rode a 60fps
		long lastTime        = System.nanoTime();
		double amountOfTicks = 60.0;
		double ns            = 1000000000 / amountOfTicks;
		double delta         = 0.0;
		
		//Pedindo para que o canvas esteja em foco
		requestFocus();
		
		while(isRunning) {
			
			//Calculo do tempo até então decorrido
			long now = System.nanoTime();
			delta   += (now - lastTime) / ns;
			lastTime = now;
			
			//Ao passar um segundo ou mais
			if ( delta >= 1) {

				tick();
				render();
				delta--;
			}
			
		}
		//Parar o programa
		try {
			stop();
		} catch (InterruptedException e) {

			e.printStackTrace();
		}
	}
	
	//Funcao que ira acontecer no fim do game
	public void stop() throws InterruptedException {
		
		isRunning = false;
		thread.join();
	}

	//Funcao que chama as acoes de todas as coisas a cada frame
	public void tick() {
		
		switch(gamestate) {
		
			case ingame:
				normalTick();
				break;
				
			case gameover:
				gameOverTick();
				break;
				
			case cutcene:
				cutceneTick();
				break;
			
			default:
				break;
		}
			
	}
	
	public void normalTick() {

		obstacles.tick();
		for (int i = 0; i < entities.size(); i++) {
			
			Entity e = entities.get(i);
			e.tick();
		}
		
	}
	
	public void gameOverTick() {
		
		framesGameOver++;
		if( framesGameOver == 90) {
			
			framesGameOver = 0;
			if (showMessageGameOver) {
				
				showMessageGameOver = false;
			}
			else {
				
				showMessageGameOver =  true;
			}
		}
		if (restartGame) {
			
			restartGame = false;
			gamestate = Gamestate.ingame;
			World.restartGame();
		}
	}
	
	public void cutceneTick() {

	}

	//Funcao para renderizar imagens na tela
	public void render() {
		
		//Iniciando buffer para renderizar imagem
		BufferStrategy bs = this.getBufferStrategy();
		if ( bs == null ) {

			this.createBufferStrategy(3);
			return;
		}
		
		Graphics g = layer.getGraphics();
		
		
		g.setColor(new Color(122,102,255));
		g.fillRect(0, 0,WIDTH,HEIGHT);
		
		Collections.sort(entities, Entity.entitySorter);
		
		//Renderizar todas as entidades na tela
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			e.render(g);
		}
		
		switch(gamestate) {
		
			case pause:
				ui.renderPause(g);
				break;
				
			case gameover:
				ui.renderGameOver(g,showMessageGameOver);
				break;
				
			default:
				ui.render(g);
				break;
				
		}

		
		//Renderizar a imagem de fundo
		g.dispose();
		g = bs.getDrawGraphics();
		
		g.drawImage(layer, 0, 0, WIDTH*SCALE, HEIGHT*SCALE, null);
		
		bs.show();
		
	}
	
	//Funcoes que coordenam as coisas que sao apertadas
	public void keyPressed(KeyEvent e) {

		if(gamestate == Gamestate.ingame) {
			playerControl(e);
			
		} else if (gamestate == Gamestate.pause) { 
			if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				gamestate = Gamestate.ingame;
				
			}
			
		} else if(gamestate == Gamestate.gameover) {
			if(e.getKeyCode() == KeyEvent.VK_ENTER) {
				restartGame = true;
				
			}
			
		}
		
	}
	
	public void playerControl(KeyEvent e) {
		System.out.println(e.getKeyCode());
		if(e.getKeyCode() == KeyEvent.VK_SPACE) 
			player.isPressed = true;
		
	}
	
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_SPACE) 
			player.isPressed = false;
	}

	public void keyTyped(KeyEvent e) {

	}
	
}