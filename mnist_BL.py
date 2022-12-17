import tensorflow as tf
from tensorflow.keras.datasets import mnist
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Conv2D
from tensorflow.keras.layers import MaxPooling2D
from tensorflow.keras.layers import Dense
from tensorflow.keras.layers import Flatten
from tensorflow.keras.optimizers import SGD
from PIL import Image
import numpy as np
from PIL import ImageOps
from numpy import argmax
from tensorflow.keras.utils import load_img
from tensorflow.keras.utils import img_to_array
from keras.models import load_model
import cv2
import sys
import os.path

# 60,000 samples of MNIST handwritten dataset
def load_dataset():
	# After loading the MNIST data, Divide into train and test datasets
	(trainX, trainY), (testX, testY) = mnist.load_data()
	# Resizing image to make it suitable for apply Convolution operation
	trainX = trainX.reshape((trainX.shape[0], 28, 28, 1))
	testX = testX.reshape((testX.shape[0], 28, 28, 1))
	# Convert class vectors to binary class matrices
	trainY = to_categorical(trainY)
	testY = to_categorical(testY)
	return trainX, trainY, testX, testY


def image_data_format(train, test):
	# Convert from integers to floats
    train = train.astype('float32')
    test = test.astype('float32')
	# Nor. 0 to 1
    train = train / 255.0
    test = test / 255.0
	# Normalized images
    train = train[:,14:,:14,:]
    test = test[:,14:,:14,:]
    return train, test

#Creating a Deep Neural Network
def define_model():
    trainX, trainY, testX, testY = load_dataset()
    # Image data formatting
    trainX, testX = image_data_format(trainX, testX)
    
    # Inititalising the CNN
    model = Sequential()
    
    # 1st Convolution Layer
    model.add(Conv2D(32, (3, 3), activation='relu', kernel_initializer='he_uniform', input_shape=(14, 14, 1)))
    model.add(MaxPooling2D((2, 2)))  # Maxpooling single maximum value of 2x2

    # 2nd Convolution Layer
    model.add(Conv2D(64, (3, 3), activation='relu', kernel_initializer='he_uniform'))
    
    # 3rd Convolution Layer
    model.add(Conv2D(64, (3, 3), activation='relu', kernel_initializer='he_uniform'))
    model.add(MaxPooling2D((2, 2)))
    
    # Before using fully connected Layer, need to be flatten so that 2D to 1D
    model.add(Flatten())
    model.add(Dense(100, activation='relu', kernel_initializer='he_uniform'))
    
    # Last Fully Connected Layer, output must be equal to number of classes, 10 (0-9)
    model.add(Dense(10, activation='softmax'))
    
    # Compiling the CNN
    opt = SGD(learning_rate=0.01, momentum=0.9)
    model.compile(optimizer=opt, loss='categorical_crossentropy', metrics=['accuracy'])
    model.summary()

    # Training the CNN on the training dataset
    model.fit(trainX, trainY, epochs=10, batch_size=32, verbose=0)

    # Evaluating on testing data set MNIST
    test_loss, test_acc = model.evaluate(testX, testY)
    print("Test loss on 10,000 test samples : ", test_loss)
    print("Validation Accuracy on 10,000 test samples : ", test_acc)
    # Save model
    model.save('Final_CNN.h5')


def process_InputImage(filename):
    # Open the image and convert into Numpy array
    im = Image.open(filename)
    na = np.array(im.convert('L'))

    # Stretch the contrast to range 0 to 255 to maximize chances of separating the digits from the background
    na = ((na.astype(np.float)-na.min())*255.0/(na.max()-na.min())).astype(np.uint8)

    print(f"max na value: {na.max()}")

    # Binarize image using a threshold value
    blk = np.array([0],  np.uint8)
    wht = np.array([255],np.uint8)
    thr = np.where(na>120, blk, wht)

    # Convert numpy array to PIL image object
    res = Image.fromarray(thr)

    # Get bounding box from binarized image
    bbox = res.getbbox()
    y = list(bbox)
    bbox = tuple(y)
    print('Bounding box:',bbox)

    # Apply bounding box to original image and save
    result = im.crop(bbox)
    result.save('result.jpeg')

    color_image = Image.open('result.jpeg')
    
    #convert the image to black and white mode with dither set to None
    bw = color_image.convert('1', dither=Image.NONE)

    # Convert to grayscale
    bw = color_image.convert('L')
    threshold = 90
    # Threshold
    bw = bw.point( lambda p: 255 if p > threshold else 0 )

    # Invert image colors so it can be read by the deep learning model
    bw = ImageOps.invert(bw)

    # Add padding to the inverted image
    bw = add_margin_to_image(bw, 50, 20, 50, 20, 0)
    bw.save(f'sample_bw.jpg')


def add_margin_to_image(image, top, right, bottom, left, color):
    width, height = image.size
    new_height = height + top + bottom
    new_width = left+ width + right
    result = Image.new(image.mode, (new_width, new_height), color)
    result.paste(image, (left, top))
    return result


def load_image(filename):
  img = load_img(filename, color_mode="grayscale", target_size=(28, 28))
  img = img_to_array(img)
  img = img.reshape(1, 28,28,1)
  img = img.astype('float32')
  img = img / 255.0
  img = img[:,14:,:14,:]
  return img


def run_example(file):
  if not os.path.isfile('Final_CNN.h5'):
    print("Training the Model")
    define_model()
  process_InputImage(file)
  img = load_image('sample_bw.jpg')
  cnn = load_model('Final_CNN.h5')
  predict_value = cnn.predict(img)
  digit = argmax(predict_value)
  print(digit)
  return digit


if __name__ == "__main__":
  fileName = sys.argv[1]
  run_example(fileName)
  cnn = load_model('Final_CNN.h5')
  converter = tf.lite.TFLiteConverter.from_keras_model(cnn)
  tflite_float_model = converter.convert()
  converter.optimizations = [tf.lite.Optimize.DEFAULT]
  tflite_quantized_model = converter.convert()

  f = open('mnist_BL.tflite', "wb")
  f.write(tflite_quantized_model)
  f.close()

    # Download the digit classification model
  from google.colab import files
  files.download('mnist_BL.tflite')