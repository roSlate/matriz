import { describe, expect, it } from "vitest";
import { ApiError, DEFAULT_ERROR_MESSAGE, errorMessage } from "./client";

describe("errorMessage", () => {
  it("gives the server's own text for an API error", () => {
    expect(errorMessage(new ApiError(409, "This user is already a member of the project."))).toBe(
      "This user is already a member of the project.",
    );
  });

  it("gives the default text for anything that is not an API error", () => {
    expect(errorMessage(new Error("boom"))).toBe(DEFAULT_ERROR_MESSAGE);
    expect(errorMessage("a string")).toBe(DEFAULT_ERROR_MESSAGE);
    expect(errorMessage(undefined)).toBe(DEFAULT_ERROR_MESSAGE);
  });

  it("gives the fallback of the caller instead of the default when one is passed", () => {
    expect(errorMessage(new Error("boom"), "Could not change the role. Please try again.")).toBe(
      "Could not change the role. Please try again.",
    );
    expect(errorMessage(new ApiError(500, "Server text"), "Unused fallback")).toBe("Server text");
  });
});
