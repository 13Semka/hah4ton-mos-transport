import random

class Camera:
    
    @staticmethod
    def take_photo(id: int):
        #тут он по адресу из 2гиса находит нужную остановку в пуле камер,
        #делает фото и мы качаем его себе, возвращаем путь
        
        return f"photo{random.randint(1, 5)}.jpg"
    