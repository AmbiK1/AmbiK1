#include <cctype>
#include <clocale>
#include <cstring>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    char text[256] = "";
    std::cout << "Введите текст: ";
    std::cin.getline(text, 256);

    int vowels = 0, consonants = 0;
    for (int i = 0; text[i] != '\0'; i++) {
        if (isalpha((unsigned char)text[i])) {
            if (strchr("aeiouyAEIOUY", text[i]) != NULL) {
                vowels++;
            } else {
                consonants++;
            }
        }
    }

    std::cout << "Гласных: " << vowels << ", согласных: " << consonants << "\n";
    if (vowels > consonants) {
        std::cout << "Гласных букв больше\n";
    } else if (consonants > vowels) {
        std::cout << "Согласных букв больше\n";
    } else {
        std::cout << "Гласных и согласных букв поровну\n";
    }
    return 0;
}
