package org.picture;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.*;


import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import java.awt.image.BufferedImage;
import java.io.*;


@MultipartConfig
public class PaintByNumbersServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, @org.jetbrains.annotations.NotNull HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!doctype html>");
        out.println("<title>Picture by numbers</title>");
        out.println("<h1>Upload picture</h1>");
        out.println("<form method='post' enctype='multipart/form-data' action='/upload'>");
        out.println("<input type='file' name='file'>");
        out.println("<label>Numbers of colors:</label>");
        out.println("<input type='number' name='n_colors' value='8'>");
        out.println("<input type='submit' value='Upload'>");
        out.println("</form>");
    }

    @Override
    public void doPost(@NotNull HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part filePart = request.getPart("file");
        String nColorsStr = request.getParameter("n_colors");
        int nColors = nColorsStr != null ? Integer.parseInt(nColorsStr) : 8;

        InputStream fileContent = filePart.getInputStream();
        BufferedImage image = ImageIO.read(fileContent);
        if (image == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid image file");
            return;
        }

        // Convert image to Mat (OpenCV format)
        Mat matImage = bufferedImageToMat(image);
        Mat reducedImage = reduceColors(matImage, nColors);

        // Convert back to BufferedImage
        BufferedImage outputImage = matToBufferedImage(reducedImage);
        response.setContentType("image/png");
        ImageIO.write(outputImage, "png", response.getOutputStream());
    }

    @NotNull
    private Mat reduceColors(Mat image, int nColors) {
        Mat reshaped = image.reshape(1, image.rows() * image.cols());
        Mat reshaped32f = new Mat();
        reshaped.convertTo(reshaped32f, CvType.CV_32F);

        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.2);
        Core.kmeans(reshaped32f, nColors, labels, criteria, 10, Core.KMEANS_PP_CENTERS, centers);

        centers.convertTo(centers, CvType.CV_8UC1);
        labels.convertTo(labels, CvType.CV_8UC1);

        Mat reduced = new Mat(image.size(), image.type());
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                int label = (int) labels.get(i * image.cols() + j, 0)[0];
                reduced.put(i, j, centers.get(label, 0));
            }
        }

        return reduced;
    }

    @NotNull
    private Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int rgb = bi.getRGB(x, y);
                byte[] bgr = new byte[]{
                        (byte) (rgb & 0xFF),
                        (byte) ((rgb >> 8) & 0xFF),
                        (byte) ((rgb >> 16) & 0xFF)
                };
                mat.put(y, x, bgr);
            }
        }
        return mat;
    }

    @NotNull
    private BufferedImage matToBufferedImage(@NotNull Mat mat) {
        BufferedImage bi = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < mat.rows(); y++) {
            for (int x = 0; x < mat.cols(); x++) {
                byte[] bgr = new byte[3];
                mat.get(y, x, bgr);
                int rgb = ((bgr[2] & 0xFF) << 16) | ((bgr[1] & 0xFF) << 8) | (bgr[0] & 0xFF);
                bi.setRGB(x, y, rgb);
            }
        }
        return bi;
    }
}
