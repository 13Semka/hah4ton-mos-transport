from fastapi import FastAPI
from ultralytics import YOLO

from Camera import Camera
from Detector import YoloDetector

path_to_model = "peopleDetectionModel.pt"
model = YOLO(path_to_model)

camera = Camera()
detector = YoloDetector(model)

app = FastAPI()

@app.get("/count_people")
def count_people(station_id):
    path_to_photo = camera.take_photo(station_id)
    count_people = detector.detect(path_to_photo)
    
    return count_people


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="127.0.0.1", port=8000)