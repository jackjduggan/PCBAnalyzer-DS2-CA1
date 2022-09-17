package com.dugganjack.pcbanalyzer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.IntStream;

// Assignment was given mark of 67%.

// Things I still want to add:
//button to go between black and white
//slider
//junit, jmh
//multiple components

public class PCBController implements Initializable {
    @FXML private TextField noiseReductionTextField;
    @FXML private Label numOfComponentsLabel;
    @FXML private MenuItem menuBarFileOpenImage;
    @FXML private Label fileNameLabel, fileSizeLabel, filePathLabel;
    @FXML private ImageView myImageView;
    private Image image;
    @FXML private MenuBar menuBar;

    WritableImage dest, destBW;

    private HashSet<Integer> components = new HashSet<>();
    int[] disjointSet;
    int numOfComp = 0;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        menuBarFileOpenImage.setOnAction(this::chooseFileAndDisplay);
    }

    /*
    Method that initialized FileChooser and calls imageReadWrite method
     */
    public void chooseFileAndDisplay(ActionEvent event) {
        //Initialize FileChooser
        FileChooser openImageChooser = new FileChooser();
        openImageChooser.setInitialDirectory(new File("C:\\Users\\jackd\\Pictures"));
        openImageChooser.setTitle("Open Image");
        openImageChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter
                        ("Image Files (.png, .jpg, .jpeg, .gif)", "*.png", "*.jpg", "*.gif", "*.JPEG"));

        //Main image read/write method
        imageReadWrite(openImageChooser);
    }

    /*
    Method that gets the hue,sat,bri of selected pixel before calling similarPixelLoop method
     */
    public void colorGrabber(MouseEvent mouseEvent) {
        Image src = myImageView.getImage();
        PixelReader pr = src.getPixelReader();

        double xPos = mouseEvent.getX();
        double yPos = mouseEvent.getY();
        int xPosition = (int) ((int) xPos / myImageView.getFitWidth() * src.getWidth());
        int yPosition = (int) ((int) yPos / myImageView.getFitHeight() * src.getHeight());

        Color col = pr.getColor(xPosition, yPosition);
        double hue = col.getHue();
        double sat = col.getSaturation();
        double bri = col.getBrightness();

        double minHue = hue * 0.7, maxHue = hue * 1.30; //30% above/below appears to be the sweet spot
        double minSat = sat * 0.7, maxSat = sat * 1.30;
        double minBri = bri * 0.7, maxBri = bri * 1.30;

        int width = (int)src.getWidth();
        int height = (int)src.getHeight();
        dest = new WritableImage(pr, width, height);
        destBW = new WritableImage(pr, width, height);
        PixelWriter pw = dest.getPixelWriter();

        similarPixelLoop(pr, minHue, maxHue, minSat, maxSat, minBri, maxBri, width, height, pw);
        myImageView.setImage(dest);

    }

    /*
    Method that closes the application
     */
    public void exitApplicationAction() {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        stage.close();
    }

    /*
    Method that sets the image to its original state
     */
    public void imageSetOriginal() {
        myImageView.setImage(image);
    }

    /*
    Method that fills in image description labels as well as initialising the imageview
     */
    public void imageReadWrite(FileChooser openImageChooser) {
        File selectedFile = openImageChooser.showOpenDialog(null);
        if (selectedFile != null) {
            fileNameLabel.setText("File Name: " + selectedFile.getName());
            fileSizeLabel.setText("File Size: " + selectedFile.length() + " bytes");
            filePathLabel.setText("File Path: " + selectedFile.getPath());

            myImageView.setFitHeight(300);
            myImageView.setFitWidth(300);
            image = new Image(selectedFile.toURI().toString(),myImageView.getFitWidth(),myImageView.getFitHeight(),false,true);
            myImageView.setImage(image);
        }
    }

    /*
    Extracted method that loops through every pixel in the image, looking for others with a similar hsb range to the
        selected pixel. If a similar pixel is found, it's made black visually (index in array). All other pixels are
        set to white (-1 in array).
     */
    private void similarPixelLoop(PixelReader pr, double minHue, double maxHue, double minSat, double maxSat,
                                  double minBri, double maxBri, int width, int height, PixelWriter pw) {

        disjointSet = new int[(width*height)]; //Array length is image width * height
        int i = 0;
        for (int y = 0; y < height; y++) { //for every pixel in the image
            for (int x = 0; x < width; x++) {
                Color color = pr.getColor(x, y);
                //If hue, saturation and brightness are NOT within the mix-max range
                if ((color.getHue() < minHue || color.getHue() > maxHue) ||
                        (color.getSaturation() < minSat || color.getSaturation() > maxSat) ||
                        (color.getBrightness() < minBri || color.getBrightness() > maxBri)) {
                    color = Color.WHITE; //set pixels to white

                } else {
                    color = Color.BLACK; //else set them to black
                }
                if(color.equals(Color.BLACK)){
                    //If the pixel is black, it's displayed as its "index" in the array
                    disjointSet[y * (int)myImageView.getImage().getWidth() + x] = i;
                } else {
                    //If the pixel is white, it's displayed as a -1 in the array
                    disjointSet[y * (int)myImageView.getImage().getWidth() + x] = -1;
                }
               pw.setColor(x, y, color);
                i++;
            }
        }
    }

    /*
    This method iterates through the pixel array and groups adjacent black pixels (left->right & above->below)
        with quick union.
     */
    public void blackPixelGrouper() {
        double IW = image.getWidth();
        for (int i = 0; i < disjointSet.length; i++) { //iterates through image pixels

            //pixel below i is also black
            if (i < disjointSet.length - IW
                    && disjointSet[i] != -1 //i is a black pixel
                    && disjointSet[(int) (i + IW)] != -1) union(disjointSet, i, (int) (i + IW));

            //pixel after i is also black
            if (i < disjointSet.length
                    && disjointSet[i] != -1  //i is a black pixel
                    && disjointSet[i + 1] != -1) union(disjointSet, i, i + 1);
        }
        addRoots();
    }

    /*
        Method that runs pixel grouping and rectangle methods when "Analyze" button is pressed.
     */
    public void analyzeImage(ActionEvent event) {
        blackPixelGrouper();
        removeRect();
        drawRect();
        System.out.println(countComponents());
    }


    //-------------------------- UNION FIND METHODS --------------------------------------------------------------------
    /*
    Iterative version of union find
     */
    public static int find(int[] a, int i) {
        if (a[i] != -1) { //if i is a black pixel
            if (a[i] != i) {
                do {
                    a[i] = a[a[i]];
                    i = a[i];
                } while (a[i] != i);
            }
            return i;
        } else { //if i is white pixel
            i = -1;
            return i;
        }
    }

    /*
    Quick union of disjoint sets containing elements p and q (V2)
     */
    public void union(int[] a, int p, int q) {
        a[find(a,q)] = find(a,p); //The root of q is made reference the root of p
    }

    /*
        Method that draws rectangles around clusters of black pixels of a certain size.
     */
    public void drawRect() {
        int counter = 0;
        int imageWidth = (int) myImageView.getImage().getWidth();
        addRoots();
        for (int j : components) {
            counter++;
            //component bounds
            double maxHeight = -1;
            double minHeight = (int) myImageView.getImage().getHeight();
            double maxWidth = -1;
            double minWidth = (int) myImageView.getImage().getWidth();

            for (int i = 0; i < disjointSet.length; i++) {
                int x = i % imageWidth;
                int y = i / imageWidth;

                if (disjointSet[i] != -1 && find(disjointSet, i) == j) { //if pixel is black AND root
                    if (x < minWidth)
                        minWidth = x;
                    if (y < minHeight)
                        minHeight = y;
                    if (x > maxWidth)
                        maxWidth = x;
                    if (y > maxHeight)
                        maxHeight = y;
                }
            }
            int num = Integer.parseInt(noiseReductionTextField.getText());
            if (sizeOfDisjointSets(j) > num) {
                numOfComp++;
                sizeOfDisjointSets(j);
                Rectangle rectangle = new Rectangle(minWidth, minHeight, maxWidth - minWidth, maxHeight - minHeight);
                rectangle.setTranslateX(myImageView.getLayoutX());
                rectangle.setTranslateY(myImageView.getLayoutY());
                ((Pane) myImageView.getParent()).getChildren().add(rectangle);
                rectangle.setStroke(Color.RED);
                rectangle.setFill(Color.TRANSPARENT);

                numOfComponentsLabel.setText("Components: " + numOfComp);

                Tooltip tooltip = new Tooltip("Disjoint Set "
                        + String.valueOf(numOfComp)
                        + "\nSize: "
                        + sizeOfDisjointSets(j)
                        + "px"
                );
                Tooltip.install(rectangle, tooltip);
            }
        }
    }

    /*
    Method that removes rectangles from imageview.
     */
    public void removeRect() {
        ArrayList<Rectangle> rectangleArrayList = new ArrayList<>(); //create a new list to store the rectangles
        for (Node rectangle : ((Pane) myImageView.getParent()).getChildren()) {
            if (rectangle instanceof Rectangle) //for all rectangles in the imageview
                rectangleArrayList.add((Rectangle) rectangle); //add them to the list
        }
        ((Pane) myImageView.getParent()).getChildren().removeAll(rectangleArrayList); //remove list contents from imageview
    }
    //ActionEvent method that calls removeRect() method.
    public void callRemoveRect(ActionEvent event) {
        removeRect();
    }

    /*
    Method that counts the total number of components (no noise reduction) in the imageview and returns the value.
     */
    public int countComponents() {
        int j = 0;

        for (int i=0; i<disjointSet.length; i++) {
            if (disjointSet[i] == i) {
                j++;
            }
        }
        return j;
    }

    /*
    Method that finds the roots of each individual disjoint sets and adds them a hashset of components.
     */
    private void addRoots() {
        for (int i=0; i<disjointSet.length; i++) {
            int p = find(disjointSet,i); //Finds roots
            if (p != -1) //if root is black
            {
                components.add(p); //adds to HashSet of components
            }
        }
    }

    /*
        Method gets the size of disjoint sets by counting number of pixels.
     */
    public int sizeOfDisjointSets(int p) {
        int numOfPixels = (int) IntStream.range(0, disjointSet.length).filter(i -> p == find(disjointSet, i)).count();
        return numOfPixels;
    }

    /*
    Method that gives each disjoint set a random color within the imageview
     */
    public void randomColorComponents() {
        Image src = myImageView.getImage();
        PixelReader pr = myImageView.getImage().getPixelReader();
        int width = (int)src.getWidth();
        int height = (int)src.getHeight();
        WritableImage newWI = new WritableImage(pr, width, height);
        PixelWriter writer = newWI.getPixelWriter();
        Random random = new Random();
        int counter = 0;
        addRoots();
        for (int j : components) {
            counter++;
            int imageWidth = (int) myImageView.getImage().getWidth();
            //generate random rgb value
            int r = random.nextInt(255);
            int g = random.nextInt(255);
            int b = random.nextInt(255);
            Color randomColor = Color.rgb(r, g, b);

            for (int i=0; i < disjointSet.length; i++) {

                if (disjointSet[i] != -1) {
                    int storedRoot = find(disjointSet, i);
                    if (j == storedRoot) { //without this it just uses the one color for every set
                        //get xy
                        int x = i % imageWidth;
                        int y = i / imageWidth;
                        writer.setColor(x, y, randomColor);
                    }

                }
            }
        }
        myImageView.setImage(newWI);
    }
    public void randomColorComponentsAction(ActionEvent event) {
        randomColorComponents();
    }

    /*
        Getters and Setters
     */
    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public HashSet<Integer> getComponents() {
        return components;
    }

    public void setComponents(HashSet<Integer> components) {
        this.components = components;
    }
}