#include <clocale>
#include <cmath>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    double a, b;
    std::cout << "Введите два числа: ";
    std::cin >> a >> b;

    double cubeMean = (a * a * a + b * b * b) / 2;
    double geomMean = sqrt(fabs(a) * fabs(b));

    std::cout << "Среднее арифметическое кубов: " << cubeMean << "\n";
    std::cout << "Среднее геометрическое модулей: " << geomMean << "\n";
    return 0;
}
