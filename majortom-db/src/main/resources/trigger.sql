CREATE TRIGGER trigger_detect_duplicate_names BEFORE INSERT OR UPDATE ON names FOR EACH ROW EXECUTE PROCEDURE detect_duplicate_names();
CREATE TRIGGER trigger_detect_duplicate_occurrences BEFORE INSERT OR UPDATE ON occurrences FOR EACH ROW EXECUTE PROCEDURE detect_duplicate_occurrences();
CREATE TRIGGER trigger_detect_duplicate_variants BEFORE INSERT OR UPDATE ON variants FOR EACH ROW EXECUTE PROCEDURE detect_duplicate_variants();