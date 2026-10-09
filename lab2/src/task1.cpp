#include <clocale>
#include <cmath>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    double x, y;
    std::cout << "Введите x: ";
    std::cin >> x;
    std::cout << "Введите y: ";
    std::cin >> y;

    double z = (fabs(x) - fabs(y)) / (1 + fabs(x * y));

    std::cout << "Результат: " << z << "\n";
    return 0;
}
