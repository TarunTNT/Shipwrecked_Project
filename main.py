# Parent class
class Animal:
    def __init__(self, name, age=5):
        self.name = name
        self.age = age

    def speak(self):
        print(f"{self.name} makes a sound.")

# Child class 1
class Dog(Animal):
    def speak(self):
        print(f"{self.name} says Woof!")

# Child class 2
class Cat(Animal):
    def speak(self):
        print(f"{self.name} says Meow!")

# Child class 3
class Bird(Animal):
    def speak(self):
        print(f"{self.name} says Tweet!, and is {self.age} years old")

# Create instances of each child class
dog = Dog("Buddy")
cat = Cat("Whiskers")
bird = Bird("Tweety")

# Call their methods
dog.speak()     # Buddy says Woof!
cat.speak()     # Whiskers says Meow!
bird.speak()    # Tweety says Tweet!
