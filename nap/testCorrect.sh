export NAP_PROGRAMS="NapExamples"
for f in "$NAP_PROGRAMS"/*.nap; do
  echo ==== Test: $(basename "$f") ====;
  java compiler.Main "$f";
done
