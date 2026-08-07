from pydantic import BaseModel
from typing import List

class UserPurchase(BaseModel):
    game_id: int
    rating: float

class UserData(BaseModel):
    user_id: int
    purchases: List[UserPurchase]
