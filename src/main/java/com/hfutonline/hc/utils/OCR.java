package com.hfutonline.hc.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @version :1.0
 * @author ChenLiangLiang
 * @date 2017/8/31 4:28:34
 */
public class OCR {

    private OCR() {
    }


    private static List<BufferedImage> cutPic(BufferedImage img) {
        List<BufferedImage> subImages = new ArrayList<>(6);

        int width = img.getWidth();
        int height = img.getHeight();
        int start = 0;
        int end = 0;
        int pointer = 0;
        int temp = 0;
        boolean flag = false;
        for (int y = 2; y < width - 2; y++) {

            temp = pointer;
            for (int x = 2; x < height - 2; x++) {
                if (identifyColor(img.getRGB(y, x)) == 1) {
                    if (!flag) {
                        flag = true;
                        start = y;
                    }
                    pointer += 1;

                    break;
                }
            }
            if (flag && pointer == temp) {
                end = y - 1;
                flag = false;
                subImages.add(cutPic1(img.getSubimage(start, 0, end - start, height)));
            }
        }

        return subImages;
    }

    private static BufferedImage cutPic1(BufferedImage img) {
        int width = img.getHeight();
        int height = img.getWidth();
        int start = 0;
        int end = 0;
        int pointer = 0;
        int temp = 0;
        boolean flag = false;
        for (int y = 2; y < width - 2; y++) {

            temp = pointer;
            for (int x = 0; x < height; x++) {
                if (identifyColor(img.getRGB(x, y)) == 1) {
                    if (!flag) {
                        flag = true;
                        start = y;
                    }
                    pointer += 1;

                    break;
                }
            }
            if (flag && pointer == temp) {
                end = y - 1;
                flag = false;
                return img.getSubimage(0, start, height, end - start);
            }
        }
        return null;
    }


    /**
     * 对背景色和验证码主色进行区分
     *
     * @param colorId 颜色标识
     * @return 0-背景色；1-验证码主色
     */
    private static int identifyColor(int colorId) {
        Color color = new Color(colorId);
        if ((color.getRed() + color.getGreen() + color.getBlue()) > 300) {
            return 0;
        } else {
            return 1;
        }

    }

    /**
     * 二值化
     *
     * @param img 验证码图片
     * @return 0-1二维数组
     */
    private static int[][] binaryZation(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] result = new int[h][w];
        for (int x = 0; x < h; x++) {
            for (int y = 0; y < w; y++) {

                result[x][y] = identifyColor(img.getRGB(y, x));

            }
        }
        return result;
    }


    /**
     * 验证码识别
     *
     * @param in 验证码字节流
     * @return 识别结果
     * @throws IOException 非图片
     */
    public static String getCode(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        return getCode(image);

    }

    /**
     * 验证码识别
     *
     * @param file 验证码文件路径
     * @return 识别结果
     * @throws IOException 文件路径错误，或文件不是图片
     */
    public static String getCode(String file) throws IOException {
        BufferedImage image = ImageIO.read(new File(file));
        return getCode(image);

    }

    /**
     * 验证码识别
     *
     * @param img 验证码图片
     * @return 识别结果
     */
    public static String getCode(BufferedImage img) {
        String result = "";
        List<BufferedImage> subImageList = cutPic(img);
        int w;
        for (BufferedImage subImage : subImageList) {
            w = subImage.getWidth();
            switch (w) {
                case 27:
                    result += "f";
                    break;
                case 29:
                    result += "5";
                    break;
                case 30:
                    result += "c";
                    break;
                case 32:
                    result += "3";
                    break;
                case 33:
                    //result+="[2,7,e,n]";
                    int[][] binaryPic = binaryZation(subImage);
                    result += identify1(binaryPic);
                    break;
                case 36:
                    //result+="[8,b,d,p,x]";
                    int[][] binaryImg = binaryZation(subImage);
                    result += identify2(binaryImg);
                    break;
                case 37:
                    result += "6";
                    break;
                case 38:
                    int[][] binaryImage = binaryZation(subImage);
                    result += identify4(binaryImage);
                    break;
                case 54:
                    result += "w";
                    break;
                case 55:
                    result += "m";
                    break;
                default:
                    result += "?";
                    break;
            }
        }

        return result;
    }

    /**
     * 识别2、7、n和e
     *
     * @param binaryPic 二值后的图片
     * @return 识别的字符
     */
    private static String identify1(int[][] binaryPic) {
        //w=33
        int h = binaryPic.length;
        int count = 0;
        for (int i = 12; i < 22; i++) {
            for (int j = h - 1; j > h - 20; j--) {
                if (binaryPic[j][i] == 1) {
                    count++;
                }
            }
        }
        if (count <= 5) {
            return "n";
        }
        count = 0;
        for (int i = h - 1; i > h - 20; i--) {
            for (int j = 32; j > 22; j--) {
                if (binaryPic[i][j] == 1) {
                    count++;
                }
            }
        }
        if (count <= 10) {
            return "7";
        }
        count = 0;
        for (int i = 20; i < 30; i++) {
            for (int j = 0; j < 10; j++) {
                if (binaryPic[i][j] == 1) {
                    count++;
                }
            }
        }
        if (count <= 5) {
            return "2";
        }
        return "e";
    }

    /**
     * 识别8、b、d、p和x
     *
     * @param binaryPic 二值后的图片
     * @return 识别的字符
     */
    private static String identify2(int[][] binaryPic) {
        //w=36
        int h = binaryPic.length;
        int count = 0;
        for (int i = 8; i < 38; i++) {
            for (int j = 27; j < 35; j++) {
                if (binaryPic[i][j] == 0) {
                    count++;
                }
            }
        }
        if (count < 5) {
            return "d";
        }
        count = 0;

        for (int i = 14; i < 35; i++) {
            for (int j = 0; j < 9; j++) {
                if (binaryPic[i][j] == 0) {
                    count++;
                }
            }
        }
        if (count < 5) {
            //识别b和p
            return identify3(binaryPic);
        }
        count = 0;

        for (int i = 0; i < 6; i++) {
            for (int j = 16; j < 24; j++) {
                if (binaryPic[i][j] == 1) {
                    count++;
                }
            }
        }
        for (int i = h - 1; i > h - 6; i--) {
            for (int j = 14; j < 22; j++) {
                if (binaryPic[i][j] == 1) {
                    count++;
                }
            }
        }
        if (count < 10) {
            return "x";
        }

        return "8";

    }

    /**
     * 识别b和p
     *
     * @param binaryPic 二值后的图片
     * @return 识别的字符
     */
    private static String identify3(int[][] binaryPic) {
        //w=36
        int h = binaryPic.length;
        int up = 0;
        int down = 0;
        for (int i = 0; i < h; i++) {
            if (binaryPic[i][10] == 1) {
                up = i + 3;
                break;
            }
        }
        for (int i = h - 1; i > 0; i--) {
            if (binaryPic[i][10] == 1) {
                down = i - 3;
                break;
            }
        }
        if ((up - h / 2) > (h / 2 - down)) {
            return "b";
        } else {
            return "p";
        }
    }


    /**
     * 识别4和y
     *
     * @param binaryPic 二值后的图片
     * @return 识别的字符
     */
    private static String identify4(int[][] binaryPic) {
        //w=33
        int h = binaryPic.length;
        int count = 0;
        for (int i = h - 1; i > h - 16; i--) {
            for (int j = 32; j > 22; j--) {
                if (binaryPic[i][j] == 1) {
                    count++;
                }
            }
        }
        if (count <= 5) {
            return "y";
        }
        return "4";
    }


}
