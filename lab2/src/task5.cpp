#include <clocale>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    int n;
    std::cout << "Введите N: ";
    std::cin >> n;

    double s = 0;
    double factI = 1;
    for (int i = 1; i <= n; i++) {
        factI = factI * i;
        double p = 1;
        double factJ = 1;
        for (int j = 1; j <= i; j++) {
            factJ = factJ * j;
            p = p * factJ / factI;
        }
        s = s + p;
    }

    std::cout << "s = " << s << "\n";
    return 0;
}
