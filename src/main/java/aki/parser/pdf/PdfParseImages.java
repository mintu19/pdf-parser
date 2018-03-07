package aki.parser.pdf;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.state.PDSoftMask;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import aki.parser.pdf.IOUtils.ImageIOUtil;

public final class PdfParseImages {

    /*
    private final static Logger LOGGER = Logger.getLogger(PdfParseImages.class.getName());
    
    // Not used now
    private final static String IMAGE_FORMAT = "jpg";   // for writing
    private final static String EXTENTION = ".jpg";     // for named extention
    //
    */

    private final static String DUMMY_FILE_NAME = "parser.aki";   // for writing

    private int MIN_W = 0;
    private int MIN_H = 0;
    private Float AR = 0f;
    private boolean ARD = false;
    private boolean VERBOSE = false;

    private final static boolean DIRECT_JPEG = false;

    private final List<String> JPEG = Arrays.asList(
            COSName.DCT_DECODE.getName(),
            COSName.DCT_DECODE_ABBREVIATION.getName());

    private final Set<COSStream> seen = new HashSet<>();
    private int imageCounter = 1;

    private CmdHelper helper;

    private OutLogHelper logHelper;

    private PdfParseImages() {
    }

    public static void main(String[] args) {
        // suppress the Dock icon on OS X
        System.setProperty("apple.awt.UIElement", "true");

        Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);

        try {
            PdfParseImages parseImages = new PdfParseImages();
            parseImages.run(args);
        } catch (ParseException | IOException | URISyntaxException e) {
            System.out.println(e.getMessage());
            CmdHelper.printHelp();
            System.exit(-1);
        }
    }

    public void run(String[] args) throws IOException, URISyntaxException, ParseException {
        
        helper = new CmdHelper(args);

        if (helper.isHelp()) {
            CmdHelper.printHelp();
            System.exit(0);
        }

        VERBOSE = helper.isVerbose();

        MIN_W = helper.getMinWidth();
        MIN_H = helper.getMinHeight();
        AR = helper.getAspectRatio();
        ARD = helper.isARDual();

        File outputDir = helper.getOutDir();

        File singleFile = helper.getSingleFile();
        if (singleFile != null) {
            // mean output dir is never null
            File outFolder = new File(outputDir, fileNameWithoutExt(singleFile.getName()));
            File dummyFile = new File(outputDir, DUMMY_FILE_NAME);
            dummyFile.createNewFile();
            initFolder(outFolder, singleFile, "a", "a");
        }

        if (helper.getDir() != null) {
            processDirectory(helper.getDir(), outputDir);
        }

        logHelper.close();
    }

    private void processDirectory(File folder, File outputDir) throws IOException, ParseException, FileNotFoundException {
        // if not single dir
        if (helper.isLogging()) {
            File outputFolder = outputDir  != null ? outputDir : folder;
            outputFolder = new File(outputFolder, "out");
            if ( ! outputFolder.isDirectory() ) {
                outputFolder.mkdir();
            }
            logHelper = new OutLogHelper(outputFolder);
        }

        long count = 0;

        if (helper.isInit()) {
            Iterator<File> dirIterator = FileUtils.iterateFilesAndDirs(folder, new NotFileFilter(TrueFileFilter.INSTANCE), TrueFileFilter.INSTANCE);
            while(dirIterator.hasNext()) {
                File directory = dirIterator.next();
                String builderName = directory.getName();
                Iterator<File> fileIterator = FileUtils.iterateFiles(directory, new WildcardFileFilter("*.pdf"), null);
                while(fileIterator.hasNext()) {
                    count++;
                    File file = fileIterator.next();
                    String projectName = fileNameWithoutExt(file.getName());
        
                    File finalOutTopDir = outputDir != null ? outputDir : directory;
                    File outDir = new File(finalOutTopDir.getAbsolutePath() + "/" + projectName);
        
                    boolean result = initFolder(outDir, file, projectName, builderName);
        
                    if (logHelper != null) {
                        logHelper.writeLog(count, result, builderName, projectName, outDir.getAbsolutePath());
                    }
                }
            }
        } else if(helper.isClean()) {
            Iterator<File> fileIterator = FileUtils.iterateFiles(folder, new WildcardFileFilter("*.pdf"), TrueFileFilter.INSTANCE);
            while(fileIterator.hasNext()) {
                count++;

                File file = fileIterator.next();
                String projectName = fileNameWithoutExt(file.getName());

                File directory = file.getParentFile();
                String builderName = directory.getName();
    
                File outDir = new File(directory.getAbsolutePath() + "/" + projectName);
    
                boolean result = cleanFolder(outDir);

                if (logHelper != null) {
                    logHelper.writeLog(count, result, builderName, projectName, outDir.getAbsolutePath());
                }
            }
        }
    }

    private String fileNameWithoutExt(String name) {
        int pos = name.lastIndexOf(".");
        return pos > 0 ? name.substring(0, pos) : name;
    }

    private boolean cleanFolder(File folder) {
        try {
            File dummyFile = new File(folder, DUMMY_FILE_NAME);
            if (dummyFile.isFile()) {
                if (VERBOSE) {
                    System.out.println("Cleaning folder: " + folder.getPath());
                }
                FileUtils.deleteDirectory(folder);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // Not using for now
    /*
    private boolean saveImagesFromResources(PDResources resources, File outDir, String name) throws IOException {

        Iterator<COSName> resIter = resources.getXObjectNames().iterator();
        while(resIter.hasNext()) {
            try {
                PDXObject xObject = resources.getXObject(resIter.next());

                if (xObject instanceof PDFormXObject) {
                    saveImagesFromResources(((PDFormXObject) xObject).getResources(), outDir, name);
                } else if (xObject instanceof PDImageXObject) {
                    PDImageXObject imageObj = ((PDImageXObject) xObject);

                    int h = imageObj.getHeight(), w = imageObj.getWidth();

                    if ( h > MIN_H && w > MIN_W ) {
                        RenderedImage image = imageObj.getImage();
                        String newName = new StringBuilder(name).append(h).append("_").append(w).append(EXTENTION).toString();
                        File outFile = new File(outDir, newName);
                        try {
                            ImageIO.write(image, IMAGE_FORMAT, outFile);
                        } catch(Exception e) {
                            System.out.println("Error in saving: " + outFile.getName());
                        }
                    }
                }
            } catch(NumberFormatException e) {
            }
        }
        return true;
    }*/

    private boolean initFolder(File outDir, File pdfFile, String projectName, String builderName) throws IOException {
        if ( ! outDir.isDirectory() ) {
            outDir.mkdir();
            File dummyFile = new File(outDir, DUMMY_FILE_NAME);
            dummyFile.createNewFile();
        }
        try {
            try(PDDocument pdDocument = PDDocument.load(pdfFile)) {
                String name = builderName + "_" + projectName + "_";
                Iterator<PDPage> pageIter = pdDocument.getPages().iterator();
                imageCounter = 1;
                while(pageIter.hasNext()) {
                    PDPage page = pageIter.next();
                    // saveImagesFromResources(page.getResources(), outDir, name);
                    ImageGraphicsEngine extractor = new ImageGraphicsEngine(page, outDir, name);
                    extractor.run();
                }
            }
        } catch(Exception e) {
            return false;
        }
        return true;
    }
    
    private class ImageGraphicsEngine extends PDFGraphicsStreamEngine {
        File outDir;
        String semiName;

        protected ImageGraphicsEngine(PDPage page, File outDir, String name) throws IOException {
            super(page);
            this.outDir = outDir;
            this.semiName = name;
        }

        public void run() throws IOException {
            PDPage page = getPage();
            processPage(page);
            PDResources res = page.getResources();
            for (COSName name : res.getExtGStateNames()) {
                PDSoftMask softMask = res.getExtGState(name).getSoftMask();
                if (softMask != null) {
                    PDTransparencyGroup group = softMask.getGroup();
                    if (group != null) {
                        processSoftMask(group);
                    }
                }
            }
        }

        @Override
        public void drawImage(PDImage pdImage) throws IOException
        {
            if (pdImage instanceof PDImageXObject) {
                if (pdImage.isStencil()) {
                    processColor(getGraphicsState().getNonStrokingColor());
                }
                PDImageXObject xobject = (PDImageXObject)pdImage;
                if (seen.contains(xobject.getCOSObject())) {
                    // skip duplicate image
                    return;
                }
                seen.add(xobject.getCOSObject());
            }

            if (checkImage(pdImage)) {
                // save image
                String name = semiName + "_" + imageCounter + "_" + pdImage.getWidth() + "x" + pdImage.getHeight();
                imageCounter++;

                if (VERBOSE) {
                    System.out.println("Writing image: " + name);            
                }
                write2file(pdImage, name, DIRECT_JPEG, outDir.getPath());
            }
        }

        private boolean checkImage(PDImage pdImage) {
            boolean flag = (pdImage.getHeight() > MIN_H && pdImage.getWidth() > MIN_W);
            float ar = pdImage.getWidth() / pdImage.getHeight();
            // Info: logical & in bw
            flag = flag && (AR != null ? (ar <= AR  || (ARD & (1/ar) <= AR)) : true);
            return flag;
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3)
                throws IOException {

        }

        @Override
        public void clip(int windingRule) throws IOException {

        }

        @Override
        public void moveTo(float x, float y) throws IOException {

        }

        @Override
        public void lineTo(float x, float y) throws IOException {

        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {

        }

        @Override
        public Point2D getCurrentPoint() throws IOException {
            return new Point2D.Float(0, 0);
        }

        @Override
        public void closePath() throws IOException {

        }

        @Override
        public void endPath() throws IOException {

        }

        @Override
        protected void showGlyph(Matrix textRenderingMatrix, 
                                 PDFont font,
                                 int code,
                                 String unicode,
                                 Vector displacement) throws IOException {
            RenderingMode renderingMode = getGraphicsState().getTextState().getRenderingMode();
            if (renderingMode.isFill()) {
                processColor(getGraphicsState().getNonStrokingColor());
            }
            if (renderingMode.isStroke()) {
                processColor(getGraphicsState().getStrokingColor());
            }
        }

        @Override
        public void strokePath() throws IOException {
            processColor(getGraphicsState().getStrokingColor());
        }

        @Override
        public void fillPath(int windingRule) throws IOException {
            processColor(getGraphicsState().getNonStrokingColor());
        }

        @Override
        public void fillAndStrokePath(int windingRule) throws IOException {
            processColor(getGraphicsState().getNonStrokingColor());
        }

        @Override
        public void shadingFill(COSName shadingName) throws IOException {

        }

        // find out if it is a tiling pattern, then process that one
        private void processColor(PDColor color) throws IOException {
            if (color.getColorSpace() instanceof PDPattern) {
                PDPattern pattern = (PDPattern) color.getColorSpace();
                PDAbstractPattern abstractPattern = pattern.getPattern(color);
                if (abstractPattern instanceof PDTilingPattern) {
                    processTilingPattern((PDTilingPattern) abstractPattern, null, null);
                }
            }
        }
    }


    private boolean hasMasks(PDImage pdImage) throws IOException {
        if (pdImage instanceof PDImageXObject) {
            PDImageXObject ximg = (PDImageXObject) pdImage;
            return ximg.getMask() != null || ximg.getSoftMask() != null;
        }
        return false;
    }

    /**
     * Writes the image to a file with the filename prefix + an appropriate suffix, like "Image.jpg".
     * The suffix is automatically set depending on the image compression in the PDF.
     * @param pdImage the image.
     * @param prefix the filename prefix.
     * @param directJPEG if true, force saving JPEG/JPX streams as they are in the PDF file. 
     * @param path the filepath to store.
     * @throws IOException When something is wrong with the corresponding file.
     */
    private void write2file(PDImage pdImage, String prefix, boolean directJPEG, String path) throws IOException {
        // Setting JPEG to always true above

        String suffix = pdImage.getSuffix();
        if (suffix == null || "jb2".equals(suffix)) {
            suffix = "png";
        } else if ("jpx".equals(suffix)) {
            // use jp2 suffix for file because jpx not known by windows
            suffix = "jp2";
        }

        String finalPath = path + File.separator + prefix + "." + suffix;
        try (FileOutputStream out = new FileOutputStream(finalPath)) {

            BufferedImage image = pdImage.getImage();
            if (image != null) {
                if ("jpg".equals(suffix)) {
                    String colorSpaceName = pdImage.getColorSpace().getName();
                    if (directJPEG || !hasMasks(pdImage) && 
                                     (PDDeviceGray.INSTANCE.getName().equals(colorSpaceName) ||
                                      PDDeviceRGB.INSTANCE.getName().equals(colorSpaceName))
                    ){
                        // RGB or Gray colorspace: get and write the unmodified JPEG stream
                        InputStream data = pdImage.createInputStream(JPEG);
                        IOUtils.copy(data, out);
                        IOUtils.closeQuietly(data);
                    } else {
                        // for CMYK and other "unusual" colorspaces, the JPEG will be converted
                        ImageIOUtil.writeImage(image, suffix, out);
                    }
                } else {
                    InputStream data = pdImage.createInputStream();
                    IOUtils.copy(data, out);
                    IOUtils.closeQuietly(data);
                }
            }
            out.flush();
        }
    }
}