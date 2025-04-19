import os

from ultralytics import YOLO


class YoloDetector:  
    
    def __init__(self, model):
        self.model = model
    
    def detect(self, path):
        results = self.model(path)

        people_count = 0
        for r in results:
            for box in r.boxes:
                if r.names[int(box.cls)] == "person":
                    people_count += 1
                    
        results[0].show()
                    
        
                   
        # пока коммент, когда не будет затычки лучше удалять фотку ибо зачем она 
        # if os.path.exists(path):
        #     os.remove(path)
            
        return people_count
                
                

                


