#include <clocale>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    double x, y;
    std::cout << "Введите координаты точки (x y): ";
    std::cin >> x >> y;

    if (x * x + y * y <= 1 && y >= -x) {
        std::cout << "Точка попадает в область\n";
    } else {
        std::cout << "Точка не попадает в область\n";
    }
    return 0;
}
