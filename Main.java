import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


public class Main extends JPanel {
	private static final long serialVersionUID = 1L;

	private BufferedImage mImg;
	
	static int length;//配列の長さ（顔の数）
	static String filename;//元のファイル名
	static int rectx[];//顔のある座標x
	static int recty[];//顔のある座標y
	static double masaic = 0.07;
	

	@Override
	public void paintComponent(Graphics g) {
		if(mImg!= null) g.drawImage(mImg, 0, 0, mImg.getWidth(), mImg.getHeight(), this);

	}

	private static Mat dObj(CascadeClassifier objDetector, Mat src) {
		Mat dst = src.clone();



		if(objDetector.empty()) throw new RuntimeException("no xml file");

		MatOfRect objDetections = new MatOfRect();

		objDetector.detectMultiScale(dst, objDetections);

		System.out.println("Detected"+objDetections.toArray().length+"faces.");
	    length = objDetections.toArray().length;
		if(objDetections.toArray().length<=0) return src;

		int i = 0;
		rectx = new int[length];
		recty = new int[length];
		for(Rect rect : objDetections.toArray()) {
			System.out.println(rect);
			//rectの値を代入
			rectx[i] = rect.x;
			recty[i] = rect.y;
			//顔を切り取る
			Rect roi = new Rect(rect.x, rect.y,rect.height,rect.width);
			Mat im2 = new Mat(src,roi);
			//顔だけファイルに出力
			Imgcodecs.imwrite("face"+ i++ +".png", im2);
		}
		masaic();
		return dst;
	}
	
	public static BufferedImage putOnPic(BufferedImage bi) throws IOException {
		BufferedImage img = bi;//元の画像
		for(int i = 0; i < length; i++) {
		      BufferedImage img2 = ImageIO.read(new File("MasaicFace"+i+".png"));//モザイクした顔だけの画像
		      Graphics2D gr = img.createGraphics();
		      gr.drawImage(img2,rectx[i],recty[i],null);
		      gr.dispose();
		}
		return img;
		
	}
	
	public static void masaic() {
		System.out.println("mosaic");
		for(int i = 0; i < length; i++) {
			Mat src2 = Imgcodecs.imread("face"+i+".png");
			Mat dst = new Mat();
			Imgproc.resize(src2, dst, new Size(), masaic, masaic, Imgproc.INTER_NEAREST);
			Imgproc.resize(dst, dst, new Size(), 1/masaic, 1/masaic, Imgproc.INTER_NEAREST);
			Imgcodecs.imwrite("MasaicFace"+i+".png", dst);
			System.out.println("Masaiced. face:"+i);
		}
	}

	public static void main(String[] args) {
		try {

			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
			//CascadeClassifier読み込む
			CascadeClassifier objDetector = new CascadeClassifier("haarcascades/haarcascade_frontalface_default.xml");
			if(objDetector.empty()) throw new RuntimeException("no xml file");//xmlファイルがなかったら実行

			VideoCapture capture = new VideoCapture(0);//ビデオキャプチャを起動

			int height = (int)capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);//画像の高さを代入
			int width = (int)capture.get(Videoio.CAP_PROP_FRAME_WIDTH);//画像の幅を代入

			if(width == 0 || height == 0) throw new Exception("Camera not found");//どちらも0だったらエラー表示

			System.out.println(width +" x "+height);//高さと幅を表示

			JFrame frame = new JFrame("camera");//フレームを初期化
			Main panel = new Main();//パネルを初期化
			

			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);//閉じた時の動作
			
			frame.setContentPane(panel);//contentPaneに設定
			frame.setVisible(true);
			//フレームのサイズをキャプチャしたビデオの大きさに調節
			frame.setSize(width+frame.getInsets().left+frame.getInsets().right, height+frame.getInsets().top+frame.getInsets().bottom);
			
			JRadioButton on = new JRadioButton("ON",true);
			JRadioButton off = new JRadioButton("OFF",false);
			panel.add(on);
			panel.add(off);
		    //Mat型からBuferedImage型へ変換
			MatToBufferedImage matToBi = new MatToBufferedImage();

			Mat capImg = new Mat();
			Mat dst = new Mat();
			while(frame.isShowing()) {
				capture.read(capImg);
				dst = dObj(objDetector,capImg);//顔だけにモザイク
				panel.mImg = putOnPic(matToBi.mat2BI(dst));//モザイクを元の画像に戻す
				panel.repaint();
			}
			capture.release();
		} catch(Exception e) {
			System.out.println(e.getMessage());
		} finally {
			System.out.println("--done--");
		}
	}
}

