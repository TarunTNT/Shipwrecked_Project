# Parent class
class Animal:
    def __init__(self, name, age=5):
        self.name = name
        self.age = age

    def speak(self):
        print(f"{self.name} makes a sound.")

# Child class 1

